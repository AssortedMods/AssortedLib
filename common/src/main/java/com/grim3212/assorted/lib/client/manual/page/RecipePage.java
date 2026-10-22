package com.grim3212.assorted.lib.client.manual.page;

import com.grim3212.assorted.lib.client.manual.ManualBook;
import com.grim3212.assorted.lib.client.manual.ManualContent;
import com.grim3212.assorted.lib.client.manual.ManualPage;
import com.grim3212.assorted.lib.client.manual.ManualPageView;
import com.grim3212.assorted.lib.client.manual.ManualRecipeLayout;
import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import com.grim3212.assorted.lib.manual.ManualRecipeView;
import com.grim3212.assorted.lib.manual.ManualSlot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.List;
import java.util.Optional;

/**
 * A recipe drawn on its station's screen. One page type covers every kind by working from a
 * {@link ManualRecipeView} and a {@link ManualRecipeLayout} rather than any one recipe class.
 * More than one recipe id cycles.
 */
public record RecipePage(Optional<Component> title, List<ResourceKey<Recipe<?>>> recipes, int interval, Optional<Component> text) implements ManualPage {

    /** The station is bottom aligned; the text gets whatever is above it. */
    private static final int BOTTOM_MARGIN = 12;

    /** Gap between the text and the station under it. */
    private static final int TEXT_GAP = 4;

    public static final MapCodec<RecipePage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ManualTextCodecs.TRANSLATABLE.optionalFieldOf("title").forGetter(RecipePage::title),
            ResourceKey.codec(Registries.RECIPE).listOf().fieldOf("recipes").forGetter(RecipePage::recipes),
            Codec.INT.optionalFieldOf("interval", 40).forGetter(RecipePage::interval),
            ManualTextCodecs.TRANSLATABLE.optionalFieldOf("text").forGetter(RecipePage::text)
    ).apply(instance, RecipePage::new));

    @Override
    public MapCodec<? extends ManualPage> codec() {
        return CODEC;
    }

    @Override
    public void render(ManualPageView view, int animationTick) {
        if (this.recipes.isEmpty()) {
            this.text.ifPresent(body -> view.text(body, 0));
            return;
        }

        ResourceKey<Recipe<?>> key = this.recipes.get(this.cycle(animationTick, this.recipes.size()));
        RecipeHolder<?> holder = SyncedRecipes.byKey(key);
        if (holder == null) {
            this.text.ifPresent(body -> view.text(body, 0));
            // Type never opted into SyncedRecipes#require, or a pack naming a recipe that is absent.
            view.centeredText(Component.translatable("gui.assortedlib.manual.recipe_missing"), view.height() / 2, view.style().errorTextColor());
            view.centeredText(Component.literal(key.identifier().toString()), view.height() / 2 + 12, view.style().mutedTextColor());
            return;
        }

        ManualRecipeView recipe = ManualRecipeView.of(holder).orElse(null);
        ManualRecipeLayout layout = ManualContent.get().layoutFor(holder.value());
        if (recipe == null || recipe.isEmpty() || layout == null) {
            this.text.ifPresent(body -> view.text(body, 0));
            view.centeredText(Component.translatable("gui.assortedlib.manual.recipe_undrawable"), view.height() / 2, view.style().errorTextColor());
            return;
        }

        this.draw(view, recipe, layout, animationTick);
    }

    private void draw(ManualPageView view, ManualRecipeView recipe, ManualRecipeLayout layout, int animationTick) {
        ContextMap context = contextMap();
        int left = (view.width() - layout.width()) / 2;

        // Bottom aligned: a taller station takes its room from the text, not off the page.
        int top = view.height() - BOTTOM_MARGIN - layout.height();

        this.text.ifPresent(body -> view.text(body, 0, top - TEXT_GAP));

        view.roundedTexture(layout.texture(), left, top, layout.u(), layout.v(), layout.width(), layout.height(),
                layout.textureWidth(), layout.textureHeight(), layout.cornerRadius());

        // Hold still while the cursor is on it.
        if (view.isMouseOver(left, top, layout.width(), layout.height())) {
            view.freezeAnimation();
        }

        for (int row = 0; row < recipe.height(); row++) {
            for (int column = 0; column < recipe.width(); column++) {
                ManualRecipeLayout.Position position = layout.input(column, row);
                if (position != null) {
                    this.drawSlot(view, recipe.slot(column, row), context, left + position.x(), top + position.y(), animationTick);
                }
            }
        }

        // The station's own slots: no part of the recipe, but part of running it.
        for (ManualRecipeLayout.Extra extra : layout.extras()) {
            this.drawSlot(view, extra.slot(), context, left + extra.position().x(), top + extra.position().y(), animationTick, extra.tooltip());
        }

        if (recipe.shapeless()) {
            ManualRecipeLayout.Position marker = layout.shapelessMarkerPosition();
            int markerX = left + marker.x();
            int markerY = top + marker.y();
            view.sprite(ManualBook.Sprites.SHAPELESS, markerX, markerY, ManualRecipeLayout.MARKER, ManualRecipeLayout.MARKER);
            view.tooltip(markerX, markerY, ManualRecipeLayout.MARKER, ManualRecipeLayout.MARKER,
                    Component.translatable("gui.assortedlib.manual.shapeless"));
        }

        ItemStack result = pick(recipe.result().stacks(context), animationTick, this.interval);
        view.item(result, left + layout.result().x(), top + layout.result().y());

        if (!result.isEmpty()) {
            view.centeredText(result.getHoverName(), top + layout.height() + 2, view.style().textColor());
        }
    }

    private void drawSlot(ManualPageView view, ManualSlot slot, ContextMap context, int x, int y, int animationTick) {
        this.drawSlot(view, slot, context, x, y, animationTick, Optional.empty());
    }

    private void drawSlot(ManualPageView view, ManualSlot slot, ContextMap context, int x, int y, int animationTick, Optional<String> tooltip) {
        List<ItemStack> stacks = slot.stacks(context);
        if (stacks.isEmpty()) {
            return;
        }

        // Gold marks a choice; the cycling alone does not say the choice is the reader's.
        if (slot.isChoice(context)) {
            view.sprite(ManualBook.Sprites.TAG_SLOT, x - 1, y - 1, ManualRecipeLayout.SLOT, ManualRecipeLayout.SLOT);
        }

        ItemStack stack = pick(stacks, animationTick, this.interval);
        // The layout's own line wins: a slot that named one has nothing to say about tags anyway.
        List<Component> notes = tooltip.map(key -> List.of(described(key, stack))).orElseGet(() -> notes(slot));
        view.item(stack, x, y, notes);
    }

    /** The layout's line for this slot, with the item in it named, as {@code Made in %s} does. */
    private static Component described(String key, ItemStack stack) {
        return Component.translatable(key, stack.getHoverName()).withStyle(ChatFormatting.GRAY);
    }

    /** Says in words what the gold ring means for this slot. */
    private static List<Component> notes(ManualSlot slot) {
        List<TagKey<Item>> tags = slot.tags();
        if (!tags.isEmpty()) {
            return tags.stream().map(tag -> (Component) Component.translatable("gui.assortedlib.manual.accepts_tag",
                    Component.literal(tag.location().toString()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY)).toList();
        }

        if (slot.display() instanceof net.minecraft.world.item.crafting.display.SlotDisplay.AnyFuel) {
            return List.of(Component.translatable("gui.assortedlib.manual.accepts_fuel").withStyle(ChatFormatting.GRAY));
        }

        return List.of();
    }

    private static ItemStack pick(List<ItemStack> stacks, int animationTick, int interval) {
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get((animationTick / Math.max(1, interval)) % stacks.size());
    }

    private int cycle(int animationTick, int size) {
        return (animationTick / Math.max(1, this.interval)) % size;
    }

    /** {@code AnyFuel} needs the level's fuel values; out of world there is nothing to draw anyway. */
    private static ContextMap contextMap() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null ? SlotDisplayContext.fromLevel(minecraft.level) : ContextMap.builder().buildAndValidate(SlotDisplayContext.CONTEXT);
    }
}
