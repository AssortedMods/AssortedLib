package com.grim3212.assorted.lib.gametest;

import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.client.manual.ManualChapter;
import com.grim3212.assorted.lib.client.manual.ManualContent;
import com.grim3212.assorted.lib.client.manual.ManualPageEntry;
import com.grim3212.assorted.lib.client.manual.ManualRecipeLayout;
import com.grim3212.assorted.lib.manual.ManualSection;
import com.grim3212.assorted.lib.client.manual.page.RecipePage;
import com.grim3212.assorted.lib.client.manual.screen.ManualScreen;
import com.grim3212.assorted.lib.client.manual.screen.ManualView;
import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Optional;

/**
 * The manual as a player uses it, with real clicks: 26.3 renumbered the mouse buttons and the book
 * stopped answering while every server test still passed. Run inside a world, as recipes sync from one.
 */
final class ManualClientTests {

    private ManualClientTests() {
    }

    static void run(ClientGameTestContext context) {
        everyRecipePageCanBeDrawn(context);
        stationTagSlotsResolveInAWorld(context);
        clicksFindTheirWayThroughTheBook(context);
    }

    /** What would draw as "recipe missing" or "undrawable": an absent recipe, or a type with no layout. */
    private static void everyRecipePageCanBeDrawn(ClientGameTestContext context) {
        // Recipes reach the client a moment after the world opens.
        context.waitFor(client -> SyncedRecipes.byKey(firstRecipe()) != null, 200);

        context.runOnClient(client -> {
            for (ManualSection section : ManualContent.get().sections()) {
                for (ManualChapter chapter : ManualContent.get().chaptersOf(section.modId())) {
                    for (ManualPageEntry entry : chapter.pages()) {
                        if (!(entry.page() instanceof RecipePage page)) {
                            continue;
                        }
                        for (ResourceKey<Recipe<?>> key : page.recipes()) {
                            RecipeHolder<?> holder = SyncedRecipes.byKey(key);
                            if (holder == null) {
                                throw new AssertionError(chapter.section() + ":" + chapter.id() + " names recipe " + key.identifier() + ", which never reached the client");
                            }
                            if (ManualContent.get().layoutFor(holder.value()) == null) {
                                throw new AssertionError("recipe " + key.identifier() + " has no manual layout for its type");
                            }
                        }
                    }
                }
            }
        });
    }

    private static ResourceKey<Recipe<?>> firstRecipe() {
        return ManualContent.get().sections().stream()
                .flatMap(section -> ManualContent.get().chaptersOf(section.modId()).stream())
                .flatMap(chapter -> chapter.pages().stream())
                .map(ManualPageEntry::page)
                .filter(RecipePage.class::isInstance)
                .flatMap(page -> ((RecipePage) page).recipes().stream())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the manual has no recipe page to check"));
    }

    /** A {@code #} tag can only be read once a world's tags are bound, which is after resources load. */
    private static void stationTagSlotsResolveInAWorld(ClientGameTestContext context) {
        context.runOnClient(client -> {
            ManualRecipeLayout.Extra extra = new ManualRecipeLayout.Extra(new ManualRecipeLayout.Position(0, 0),
                    new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString("{\"type\": \"minecraft:tag\", \"tag\": \"#minecraft:logs\"}")),
                    0, Optional.empty());
            List<ItemStack> stacks = extra.slot().stacks(SlotDisplayContext.fromLevel(client.level));
            if (stacks.stream().noneMatch(stack -> stack.is(Items.OAK_LOG))) {
                throw new AssertionError("a station's #minecraft:logs slot showed " + stacks);
            }
        });
    }

    private static void clicksFindTheirWayThroughTheBook(ClientGameTestContext context) {
        context.setScreen(() -> new ManualScreen(new ManualView.Index()));
        context.waitForScreen(ManualScreen.class);
        context.waitTick();

        clickRow(context, "the index");
        expectView(context, ManualView.Chapters.class);

        clickRow(context, "a chapter list");
        expectView(context, ManualView.Pages.class);

        ManualView.Pages before = (ManualView.Pages) view(context);
        clickWidget(context, "nextButton");
        ManualView.Pages after = (ManualView.Pages) view(context);
        if (after.leftPage() <= before.leftPage()) {
            throw new AssertionError("the next page arrow left the book at page " + after.leftPage());
        }

        clickWidget(context, "homeButton");
        expectView(context, ManualView.Index.class);

        context.setScreen(() -> null);
    }

    /** Clicks the first row on the open spread: a section on the index, a chapter in a section. */
    private static void clickRow(ClientGameTestContext context, String where) {
        int[] at = context.computeOnClient(client -> {
            List<?> rows = (List<?>) read(client.gui.screen(), "rows");
            if (rows.isEmpty()) {
                throw new AssertionError(where + " has no rows to click");
            }
            Object row = rows.getFirst();
            return new int[]{(int) component(row, "x") + 2, (int) component(row, "y") + 2};
        });
        click(context, at[0], at[1]);
    }

    private static void clickWidget(ClientGameTestContext context, String field) {
        int[] at = context.computeOnClient(client -> {
            AbstractWidget widget = (AbstractWidget) read(client.gui.screen(), field);
            return new int[]{widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2};
        });
        click(context, at[0], at[1]);
    }

    /** Moves the real cursor there in window pixels and presses the left button, as a player would. */
    private static void click(ClientGameTestContext context, int guiX, int guiY) {
        double[] pixels = context.computeOnClient(client -> {
            Window window = client.getWindow();
            return new double[]{(guiX + 0.5) * window.getScreenWidth() / window.getGuiScaledWidth(),
                    (guiY + 0.5) * window.getScreenHeight() / window.getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(pixels[0], pixels[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
    }

    private static void expectView(ClientGameTestContext context, Class<? extends ManualView> expected) {
        ManualView view = view(context);
        if (!expected.isInstance(view)) {
            throw new AssertionError("expected the book at " + expected.getSimpleName() + " after a click, but it is at " + view);
        }
    }

    private static ManualView view(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            if (!(client.gui.screen() instanceof ManualScreen)) {
                throw new AssertionError("the manual closed, the screen is " + client.gui.screen());
            }
            return (ManualView) read(client.gui.screen(), "view");
        });
    }

    /** The screen keeps where it is and what it drew to itself; these tests are the only outside reader. */
    private static Object read(Object screen, String name) {
        try {
            Field field = ManualScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ManualScreen has no field " + name, e);
        }
    }

    private static Object component(Object record, String name) {
        try {
            for (RecordComponent component : record.getClass().getRecordComponents()) {
                if (component.getName().equals(name)) {
                    component.getAccessor().setAccessible(true);
                    return component.getAccessor().invoke(record);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("could not read a row's " + name, e);
        }
        throw new AssertionError("a row has no " + name);
    }
}
