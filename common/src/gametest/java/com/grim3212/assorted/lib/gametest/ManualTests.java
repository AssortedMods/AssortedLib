package com.grim3212.assorted.lib.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.client.manual.ManualBookStyle;
import com.grim3212.assorted.lib.client.manual.ManualChapter;
import com.grim3212.assorted.lib.client.manual.ManualPageEntry;
import com.grim3212.assorted.lib.client.manual.page.TextPage;
import com.grim3212.assorted.lib.conditions.DisplayCondition;
import com.grim3212.assorted.lib.conditions.DisplayConditions;
import com.grim3212.assorted.lib.conditions.LibParts;
import com.grim3212.assorted.lib.core.item.LibDataComponents;
import com.grim3212.assorted.lib.client.manual.ManualRecipeLayout;
import com.grim3212.assorted.lib.manual.LibItems;
import com.grim3212.assorted.lib.manual.ManualLinks;
import com.grim3212.assorted.lib.manual.ManualPageRef;
import com.grim3212.assorted.lib.manual.ManualRegistry;
import com.grim3212.assorted.lib.manual.ManualSection;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.grim3212.assorted.lib.test.TestSupport.survivalPlayer;
import static com.grim3212.assorted.lib.test.TestSupport.tooltipKeys;

/**
 * The parts of the manual a dedicated server can see: the page addresses, the section registry and
 * the lookups that decide where right clicking something lands. The book itself is client only and
 * is not covered here.
 */
final class ManualTests {

    /** The recipe layouts this mod ships, all for vanilla's own stations. */
    private static final List<String> LAYOUTS =
            List.of("crafting", "smelting", "blasting", "smoking", "campfire_cooking", "stonecutting");

    private static final ManualPageRef ANVIL_PAGE = ManualPageRef.of("assortedlibtest", "blocks", "anvil");
    private static final ManualPageRef APPLE_PAGE = ManualPageRef.of("assortedlibtest", "food", "apple");
    private static final ManualPageRef PIG_PAGE = ManualPageRef.of("assortedlibtest", "animals", "pig");

    private static final String PART_ON = "assortedlibtest_on";
    private static final String PART_OFF = "assortedlibtest_off";

    private ManualTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("manual_page_refs_round_trip", ManualTests::pageRefsRoundTrip);
        out.accept("manual_page_refs_reject_nonsense", ManualTests::pageRefsRejectNonsense);
        out.accept("manual_links_resolve_by_kind", ManualTests::linksResolveByKind);
        out.accept("manual_links_read_framed_items", ManualTests::linksReadFramedItems);
        out.accept("manual_takes_the_click_off_a_frame", ManualTests::manualTakesTheClickOffAFrame);
        out.accept("manual_parts_hide_chapters_and_pages", ManualTests::partsHideChaptersAndPages);
        out.accept("manual_conditions_read_and_compose", ManualTests::conditionsReadAndCompose);
        out.accept("manual_sections_sort_by_index", ManualTests::sectionsSortByIndex);
        out.accept("manual_section_is_registered", ManualTests::sectionIsRegistered);
        out.accept("manual_section_icon_takes_the_first_registered", ManualTests::sectionIconTakesTheFirstRegistered);
        out.accept("manual_codecs_read_the_shipped_data", ManualTests::codecsReadTheShippedData);
        out.accept("manual_tooltip_counts_sections", ManualTests::tooltipCountsSections);
    }

    /** A reference survives being written out and read back, with and without a page. */
    private static void pageRefsRoundTrip(GameTestHelper helper) {
        ManualPageRef page = ManualPageRef.of("assortedcore", "machines", "alloy_forge");
        assertEquals(helper, "assortedcore:machines/alloy_forge", page.toString(), "page reference text");
        assertEquals(helper, page, ManualPageRef.parse(page.toString()), "page reference round trip");

        ManualPageRef chapter = ManualPageRef.ofChapter("assortedcore", "machines");
        assertEquals(helper, "assortedcore:machines", chapter.toString(), "chapter reference text");
        assertEquals(helper, chapter, ManualPageRef.parse(chapter.toString()), "chapter reference round trip");
        if (!chapter.isChapterOnly()) {
            helper.fail("A reference with no page should read as chapter only");
        }

        helper.succeed();
    }

    /** Anything that is not {@code section:chapter[/page]} is refused rather than half parsed. */
    private static void pageRefsRejectNonsense(GameTestHelper helper) {
        for (String bad : new String[]{"", "nocolon", ":chapter", "section:", "section:/page", "section:chapter/"}) {
            if (ManualPageRef.read(bad).result().isPresent()) {
                helper.fail("Expected '" + bad + "' to be refused as a page reference");
            }
        }

        helper.succeed();
    }

    /** A block, an item and an entity each find the page they were linked to, and nothing else does. */
    private static void linksResolveByKind(GameTestHelper helper) {
        ManualLinks.link(Blocks.ANVIL, ANVIL_PAGE);
        ManualLinks.link(Items.APPLE, APPLE_PAGE);
        ManualLinks.link(EntityTypes.PIG, PIG_PAGE);

        BlockState anvil = Blocks.ANVIL.defaultBlockState();
        assertEquals(helper, ANVIL_PAGE, ManualLinks.pageFor(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), anvil), "block link");
        assertEquals(helper, APPLE_PAGE, ManualLinks.pageFor(new ItemStack(Items.APPLE)), "item link");

        var pig = helper.spawn(EntityTypes.PIG, BlockPos.ZERO);
        assertEquals(helper, PIG_PAGE, ManualLinks.pageFor(pig), "entity link");

        if (ManualLinks.pageFor(new ItemStack(Items.STONE)) != null) {
            helper.fail("An unlinked item should have no page");
        }
        if (ManualLinks.pageFor(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), Blocks.STONE.defaultBlockState()) != null) {
            helper.fail("An unlinked block should have no page");
        }

        helper.succeed();
    }

    /** A frame reads the item on display, and an empty one reads as nothing. */
    private static void linksReadFramedItems(GameTestHelper helper) {
        ManualLinks.link(Items.APPLE, APPLE_PAGE);

        ItemFrame frame = frameHolding(helper, ItemStack.EMPTY);
        if (ManualLinks.pageFor(frame) != null) {
            helper.fail("An empty frame should have no page");
        }

        frame.setItem(new ItemStack(Items.APPLE));
        assertEquals(helper, APPLE_PAGE, ManualLinks.pageFor(frame), "framed item link");
        helper.succeed();
    }

    /**
     * The interaction the book takes before the frame's own: a frame holding something with a page
     * is read rather than rotated, so the click reaches the book at all.
     */
    private static void manualTakesTheClickOffAFrame(GameTestHelper helper) {
        ManualLinks.link(Items.APPLE, APPLE_PAGE);

        ItemFrame frame = frameHolding(helper, new ItemStack(Items.APPLE));
        ServerPlayer player = survivalPlayer(helper, new ItemStack(LibItems.INSTRUCTION_MANUAL.get()));

        InteractionResult result = player.interactOn(frame, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(result.consumesAction(), "the manual did not take the interaction, got " + result);
        helper.assertValueEqual(frame.getRotation(), 0, "frame rotation after the manual was used on it");

        // A frame with nothing to read is left to vanilla, which is what lets the book be framed.
        ItemFrame empty = frameHolding(helper, ItemStack.EMPTY);
        player.interactOn(empty, InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(empty.getItem().is(LibItems.INSTRUCTION_MANUAL.get()),
                "an empty frame should still take the book, it holds " + empty.getItem());

        helper.succeed();
    }

    private static ItemFrame frameHolding(GameTestHelper helper, ItemStack held) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO.above());
        ItemFrame frame = new ItemFrame(helper.getLevel(), pos, Direction.NORTH);
        frame.setItem(held);
        helper.getLevel().addFreshEntity(frame);
        return frame;
    }

    /**
     * Resolving applies the conditions once: a hidden chapter leaves the book, a hidden page leaves
     * the chapter, and the pages after it close up so nothing addresses a page that is not there.
     */
    private static void partsHideChaptersAndPages(GameTestHelper helper) {
        registerTestParts();

        ManualPageEntry always = pageEntry("always", List.of());
        ManualPageEntry off = pageEntry("off", List.of(new DisplayConditions.PartEnabled(PART_OFF)));
        ManualPageEntry last = pageEntry("last", List.of(new DisplayConditions.PartEnabled(PART_ON)));

        ManualChapter raw = chapter("parts", List.of(), List.of(always, off, last));
        List<ManualChapter> resolved = ManualChapter.resolve(List.of(raw));
        assertEquals(helper, 1, resolved.size(), "resolved chapter count");

        ManualChapter chapter = resolved.getFirst();
        assertEquals(helper, 2, chapter.pageCount(), "visible page count");
        assertEquals(helper, "always", chapter.page(0).flatMap(ManualPageEntry::id).orElse(""), "first visible page");
        assertEquals(helper, "last", chapter.page(1).flatMap(ManualPageEntry::id).orElse(""), "second visible page");

        // The hidden page is gone rather than blank, so the one after it moves up into its place.
        assertEquals(helper, 1, chapter.indexOfPage("last"), "index of the page after a hidden one");
        // A link to a hidden page falls back to the chapter rather than opening nothing.
        assertEquals(helper, 0, chapter.indexOfPage("off"), "index of a hidden page");

        // A chapter whose own condition fails, and one left with no page, both leave the book.
        ManualChapter hidden = chapter("hidden", List.of(new DisplayConditions.PartEnabled(PART_OFF)), List.of(always));
        assertEquals(helper, 0, ManualChapter.resolve(List.of(hidden)).size(), "a chapter conditioned off");
        ManualChapter emptied = chapter("emptied", List.of(), List.of(off));
        assertEquals(helper, 0, ManualChapter.resolve(List.of(emptied)).size(), "a chapter with every page hidden");

        // Reading a resolved chapter must not ask a condition again; the book draws from these.
        int before = PART_CHECKS.get();
        for (int i = 0; i < 20; i++) {
            chapter.pageCount();
            chapter.page(0);
            chapter.indexOfPage("last");
        }
        assertEquals(helper, before, PART_CHECKS.get(), "condition checks while reading a resolved chapter");

        helper.succeed();
    }

    private static ManualChapter chapter(String id, List<DisplayCondition> conditions, List<ManualPageEntry> pages) {
        return new ManualChapter.Definition(Optional.empty(), Optional.empty(), 0, conditions, pages)
                .bind("assortedlibtest", id);
    }

    private static ManualPageEntry pageEntry(String id, List<DisplayCondition> conditions) {
        return new ManualPageEntry(Optional.of(id), conditions,
                new TextPage(Optional.empty(), Component.literal(id)));
    }

    /**
     * The conditions a pack can name are not all about parts: they compose, and they round trip
     * through the same json shape a recipe's load conditions use.
     */
    private static void conditionsReadAndCompose(GameTestHelper helper) {
        registerTestParts();
        DisplayConditions.bootstrap();

        DisplayCondition on = new DisplayConditions.PartEnabled(PART_ON);
        DisplayCondition off = new DisplayConditions.PartEnabled(PART_OFF);

        assertEquals(helper, true, on.test(), "a part that is on");
        assertEquals(helper, false, off.test(), "a part that is off");
        assertEquals(helper, true, new DisplayConditions.Not(off).test(), "not of a failing condition");
        assertEquals(helper, false, new DisplayConditions.AllOf(List.of(on, off)).test(), "all_of with one failing");
        assertEquals(helper, true, new DisplayConditions.AnyOf(List.of(on, off)).test(), "any_of with one passing");
        assertEquals(helper, false, new DisplayConditions.AnyOf(List.of()).test(), "any_of of nothing");
        assertEquals(helper, true, new DisplayConditions.AllOf(List.of()).test(), "all_of of nothing");

        // A mod that is loaded, and one that is not, without naming a part at all.
        assertEquals(helper, true, new DisplayConditions.ModLoaded("assortedlib").test(), "a loaded mod");
        assertEquals(helper, false, new DisplayConditions.ModLoaded("not_a_real_mod").test(), "an absent mod");
        assertEquals(helper, true, new DisplayConditions.ItemExists(Identifier.withDefaultNamespace("apple")).test(),
                "an item that exists");
        assertEquals(helper, false,
                new DisplayConditions.ItemExists(Identifier.withDefaultNamespace("not_a_real_item")).test(),
                "an item that does not");

        // Nesting survives being written out and read back, which is what a resource pack does.
        DisplayCondition nested = new DisplayConditions.Not(new DisplayConditions.AnyOf(List.of(off, on)));
        JsonElement json = DisplayConditions.CODEC.encodeStart(JsonOps.INSTANCE, nested).getOrThrow();
        DisplayCondition read = DisplayConditions.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(helper, nested.test(), read.test(), "a nested condition read back");
        if (!json.getAsJsonObject().get("type").getAsString().equals("assortedlib:not")) {
            helper.fail("A condition should write its type the way a recipe condition does, got " + json);
        }

        helper.succeed();
    }

    /** Counts how often a part was asked, so a test can show that reading does not ask again. */
    private static final AtomicInteger PART_CHECKS = new AtomicInteger();

    private static synchronized void registerTestParts() {
        if (!LibParts.isRegistered(PART_ON)) {
            LibParts.register(PART_ON, () -> {
                PART_CHECKS.incrementAndGet();
                return true;
            });
        }
        if (!LibParts.isRegistered(PART_OFF)) {
            LibParts.register(PART_OFF, () -> {
                PART_CHECKS.incrementAndGet();
                return false;
            });
        }
    }

    /**
     * The index is ordered by the number a section carries, not by when it loaded, so a resource
     * pack can move a mod by shipping its own {@code section.json}. Ties fall back to the mod id.
     */
    private static void sectionsSortByIndex(GameTestHelper helper) {
        ManualSection lib = new ManualSection("assortedlib", ManualSection.LIB_SORT_ORDER, () -> ItemStack.EMPTY);
        ManualSection core = new ManualSection("assortedcore", 20, () -> ItemStack.EMPTY);
        ManualSection world = new ManualSection("assortedworld", 140, () -> ItemStack.EMPTY);
        ManualSection tie = new ManualSection("assortedaaa", 20, () -> ItemStack.EMPTY);

        List<String> order = Stream.of(world, core, tie, lib)
                .sorted(Comparator.comparingInt(ManualSection::sortOrder).thenComparing(ManualSection::modId))
                .map(ManualSection::modId)
                .toList();
        assertEquals(helper, List.of("assortedlib", "assortedaaa", "assortedcore", "assortedworld"), order,
                "section order");

        // The library sorts above a mod that named no index at all.
        if (ManualSection.LIB_SORT_ORDER >= ManualSection.DEFAULT_SORT_ORDER) {
            helper.fail("The library's section has to sort above a section that took the default");
        }
        // The shipped indices leave room to slot a mod between two neighbours.
        if (core.sortOrder() + 1 >= world.sortOrder()) {
            helper.fail("Section indices are spaced too tightly to insert between them");
        }

        helper.succeed();
    }

    /**
     * The code path a mod can use instead of shipping a {@code section.json}. The library's own
     * section is data, so this registers one of its own rather than looking for it.
     */
    private static void sectionIsRegistered(GameTestHelper helper) {
        if (LibItems.INSTRUCTION_MANUAL.get() == null) {
            helper.fail("The instruction manual item should be registered");
        }

        String modId = "assortedlibtest";
        ManualRegistry.register(ManualSection.of(modId, () -> new ItemStack(LibItems.INSTRUCTION_MANUAL.get())));

        ManualSection registered = ManualRegistry.section(modId).orElse(null);
        if (registered == null) {
            helper.fail("A section registered in code should be readable back");
        } else if (!ManualRegistry.sections().contains(registered)) {
            helper.fail("A registered section should appear in the index order");
        }

        helper.succeed();
    }

    /** Mods sharing a section each ship its file naming all their items, so a missing one is skipped. */
    private static void sectionIconTakesTheFirstRegistered(GameTestHelper helper) {
        ManualSection.Definition shared = ManualSection.Definition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"sort_order\": 70, \"icon\": [\"assortedlibtest:not_installed\", \"minecraft:clock\"]}")).getOrThrow();
        helper.assertTrue(shared.bind("assortedlibtest").icon().get().is(Items.CLOCK), "the first icon that is registered is drawn");

        ManualSection.Definition single = ManualSection.Definition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"icon\": \"minecraft:clock\"}")).getOrThrow();
        helper.assertTrue(single.bind("assortedlibtest").icon().get().is(Items.CLOCK), "a single icon still reads");

        ManualSection.Definition none = ManualSection.Definition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"icon\": [\"assortedlibtest:not_installed\"]}")).getOrThrow();
        helper.assertTrue(none.bind("assortedlibtest").icon().get().is(Items.BOOK), "with none installed the section falls back to a book");
        helper.succeed();
    }

    /**
     * The codecs the book is described with, run over the files the library ships. As much about
     * the codecs themselves as about the data: one built from a static field declared below it
     * reads that field as null and takes the whole class down with it, which on a client shows up
     * only as a resource reload dying half way.
     */
    private static void codecsReadTheShippedData(GameTestHelper helper) {
        parse(helper, ManualBookStyle.CODEC, "{}", "an empty book style");
        parse(helper, ManualBookStyle.CODEC,
                "{\"width\": 400, \"colors\": {\"text\": \"#112233\", \"title\": \"#FF112233\"}}",
                "a book style overriding some fields");

        ManualBookStyle style = ManualBookStyle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        if (style.textColor() != ManualBookStyle.DEFAULT.textColor() || style.width() != ManualBookStyle.DEFAULT.width()) {
            helper.fail("An empty book style should read as the defaults");
        }

        for (String bad : new String[]{"{\"colors\": {\"text\": \"nonsense\"}}", "{\"colors\": {\"text\": \"#12\"}}"}) {
            if (ManualBookStyle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).result().isPresent()) {
                helper.fail("Expected '" + bad + "' to be refused as a book style");
            }
        }

        parse(helper, ManualSection.Definition.CODEC, resource(helper, "/assets/assortedlib/manual/section.json"), "the library's section");

        for (String layout : LAYOUTS) {
            String path = "/assets/minecraft/manual/recipe_layouts/" + layout + ".json";
            parse(helper, ManualRecipeLayout.CODEC, resource(helper, path), path);
        }

        helper.succeed();
    }

    private static <T> void parse(GameTestHelper helper, Codec<T> codec, String json, String what) {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        if (result.error().isPresent()) {
            helper.fail("Could not read " + what + ": " + result.error().get().message());
        }
    }

    private static String resource(GameTestHelper helper, String path) {
        try (InputStream in = ManualTests.class.getResourceAsStream(path)) {
            if (in == null) {
                throw helper.assertionException(path + " is not on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw helper.assertionException("could not read " + path + ": " + e);
        }
    }

    private static void assertEquals(GameTestHelper helper, Object expected, Object actual, String what) {
        if (!expected.equals(actual)) {
            helper.fail("Wrong " + what + ": expected " + expected + " but got " + actual);
        }
    }

    /** The book's section line comes from its component; a dedicated server has no book to count. */
    private static void tooltipCountsSections(GameTestHelper helper) {
        ItemStack manual = new ItemStack(LibItems.INSTRUCTION_MANUAL.get());
        assertEquals(helper, List.of("gui.assortedlib.manual.sections"), tooltipKeys(helper, manual, LibDataComponents.MANUAL_SECTIONS.get()), "manual tooltip");
        helper.succeed();
    }
}
