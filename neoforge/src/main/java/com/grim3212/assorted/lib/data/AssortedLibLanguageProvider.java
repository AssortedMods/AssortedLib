package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.LibConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;

/**
 * Names for the c: item tags AssortedLib declares and neither loader names, and the manual's own
 * text. Every mod loads this file, so a mod that uses one of these tags needs no name for it.
 */
public class AssortedLibLanguageProvider extends LibLanguageProvider {

    /** A blank line between paragraphs; the manual splits its text the way the font does. */
    private static final String BREAK = "\n\n";

    public AssortedLibLanguageProvider(PackOutput output) {
        super(output, LibConstants.MOD_ID);
    }

    @Override
    protected void addNames() {
        for (DyeColor color : DyeColor.values()) {
            String name = titleCase(color.getName());
            this.add("tag.item.c.glass_blocks." + color.getName(), name + " Glass Blocks");
            this.add("tag.item.c.glass_panes." + color.getName(), name + " Glass Panes");
        }
        this.add("tag.item.c.containers.fluid", "Fluid Containers");
        this.add("tag.item.c.dusts.prismarine", "Prismarine Dusts");
        this.add("assortedlib.info.combo", "Combo: %s");
        this.add("key.assortedlib.switch_modes", "Switch Tool Modes");
        this.add("key.category.assortedlib.general", "Assorted Mods");
        this.add("assortedlib.info.locked", "Locked");
        this.add("assortedlib.info.storage_level", "Storage Level %s");
        this.add("tag.item.c.keys", "Keys");
        this.add("tag.item.c.locks", "Locks");
        this.add("tag.item.c.storage_blocks.amethyst", "Amethyst Storage Blocks");
        this.add("tag.item.c.storage_blocks.quartz", "Quartz Storage Blocks");

        this.addManualScreen();
        this.addManualContent();
    }

    /** The book's furniture: its title, its controls, and what it says when something is off. */
    private void addManualScreen() {
        this.add("gui.assortedlib.manual.title", "Instruction Manual");
        this.add("gui.assortedlib.manual.index.intro",
                "Every Assorted mod you have installed writes its own part of this book." + BREAK
                        + "Pick a mod from the main page to see what it adds, then pick a chapter to read it." + BREAK
                        + "Out in the world, right click a block, item or creature with the manual and it opens straight to that page.");
        this.add("gui.assortedlib.manual.index.empty",
                "Nothing has been written yet. Install one of the Assorted mods and its chapters appear here.");
        this.add("gui.assortedlib.manual.chapter_missing", "This chapter is no longer part of the book.");
        this.add("gui.assortedlib.manual.recipe_missing", "Recipe not found");
        this.add("gui.assortedlib.manual.recipe_undrawable", "This recipe cannot be drawn here.");
        this.add("gui.assortedlib.manual.page_indicator", "%s / %s");
        this.add("gui.assortedlib.manual.previous", "Back");
        this.add("gui.assortedlib.manual.next", "Next page");
        this.add("gui.assortedlib.manual.index", "Back to the index");
        this.add("gui.assortedlib.manual.sections", "Covers %s mod(s)");
        this.add("gui.assortedlib.manual.accepts_tag", "Accepts any %s");
        this.add("gui.assortedlib.manual.accepts_fuel", "Accepts any furnace fuel");
        this.add("gui.assortedlib.manual.shapeless", "Shapeless Recipe");
    }

    /** The library's own section, which explains the book itself. */
    private void addManualContent() {
        this.add("manual.assortedlib.title", "The Manual");
        this.add("manual.assortedlib.description",
                "This first section is about the book in your hands.");

        this.add("manual.assortedlib.chapter.reading", "Reading the Manual");

        this.add("manual.assortedlib.chapter.reading.pointing.title", "Pointing at Things");
        this.add("manual.assortedlib.chapter.reading.pointing",
                "Right click anything with the manual and it opens to that page, if it has one. Blocks, dropped items and creatures all work." + BREAK
                        + "An item drawn on a page is a link. Click it and the book turns to what it is." + BREAK
                        + "The arrows at the corners turn the page, and step back out to the chapters and the index.");

        this.add("manual.assortedlib.chapter.reading.recipes.title", "Reading Recipes");
        this.add("manual.assortedlib.chapter.reading.recipes",
                "A recipe is drawn on the screen of whatever makes it, so crafting looks like a crafting table and smelting like a furnace." + BREAK
                        + "The slots a container always has are drawn too. For example a furnace shows its fuel.");

        this.add("manual.assortedlib.chapter.reading.marks.title", "Marks on a Recipe");
        this.add("manual.assortedlib.chapter.reading.marks",
                "A slot ringed in gold takes any one of several things, and cycles through them. This usually means just the item tag that the recipe consists of." + BREAK
                        + "Two crossed arrows in the top-right corner mean the pieces go in any arrangement and the recipe is Shapeless." + BREAK
                        + "Hovering your cursor on a recipe and it stops cycling while you read.");

        this.add("manual.assortedlib.chapter.reading.crafting.title", "The Manual");
        this.add("manual.assortedlib.chapter.reading.crafting",
                "A book, a piece of leather to bind it and any dye for the ink.");
    }
}
