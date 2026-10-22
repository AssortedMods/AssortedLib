package com.grim3212.assorted.lib.test;

import com.grim3212.assorted.lib.client.manual.ManualChapter;
import com.grim3212.assorted.lib.client.manual.ManualContent;
import com.grim3212.assorted.lib.client.manual.ManualPageEntry;
import com.grim3212.assorted.lib.client.manual.ManualRecipeLayout;
import com.grim3212.assorted.lib.client.manual.page.RecipePage;
import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import com.grim3212.assorted.lib.manual.ManualSection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** What the loaded manual would draw as missing or empty, which only shows on a client in a world, as recipes and tags arrive from one. */
public final class ManualCheck {

    private ManualCheck() {
    }

    /** True once the server's recipes have reached the client, which they do all at once; wait for it before {@link #problems()}. */
    public static boolean recipesHaveSynced() {
        return !SyncedRecipes.recipes().values().isEmpty();
    }

    /** Recipe pages left with no recipe to draw or none they can lay out, and station slots that show nothing. */
    public static List<String> problems() {
        List<String> problems = new ArrayList<>();
        ManualContent content = ManualContent.get();

        for (ManualSection section : content.sections()) {
            for (ManualChapter chapter : content.chaptersOf(section.modId())) {
                for (ManualPageEntry entry : chapter.pages()) {
                    if (!entry.isVisible() || !(entry.page() instanceof RecipePage page)) {
                        continue;
                    }
                    // The page skips a recipe an absent optional mod would add, so only a page left with none is an error.
                    List<RecipeHolder<?>> present = page.recipes().stream().<RecipeHolder<?>>map(SyncedRecipes::byKey).filter(Objects::nonNull).toList();
                    if (!page.recipes().isEmpty() && present.isEmpty()) {
                        problems.add(chapter.section() + ":" + chapter.id() + " has a page whose recipes never reached the client: " + page.recipes());
                    }
                    for (RecipeHolder<?> holder : present) {
                        if (content.layoutFor(holder.value()) == null) {
                            problems.add("recipe " + holder.id().identifier() + " has no manual layout for its type");
                        }
                    }
                }
            }
        }

        ContextMap context = SlotDisplayContext.fromLevel(Minecraft.getInstance().level);
        for (RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE) {
            ManualRecipeLayout layout = content.layout(type);
            if (layout == null) {
                continue;
            }
            for (ManualRecipeLayout.Extra extra : layout.extras()) {
                if (extra.slot().stacks(context).isEmpty()) {
                    problems.add("the " + BuiltInRegistries.RECIPE_TYPE.getKey(type) + " layout has a slot that shows nothing: " + extra.display().getValue());
                }
            }
        }

        return problems;
    }
}
