package com.grim3212.assorted.lib.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.locale.Language;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.platform.services.IPlatformHelper;
import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.grim3212.assorted.lib.gametest.LibTestSupport.*;

/**
 * The platform services resolve to this loader's implementations and answer like vanilla: registries, tool tiers, level properties and common tags.
 */
final class PlatformServiceTests {

    private PlatformServiceTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("services_all_resolve", PlatformServiceTests::servicesAllResolve);
        out.accept("registry_round_trips", PlatformServiceTests::registryRoundTrips);
        out.accept("tiered_tool_agrees_with_vanilla", PlatformServiceTests::tieredToolAgreesWithVanilla);
        out.accept("tool_tiers_read_the_same_for_every_tool_type", PlatformServiceTests::toolTiersReadTheSameForEveryToolType);
        out.accept("level_properties_match_level", PlatformServiceTests::levelPropertiesMatchLevel);
        out.accept("common_tags_are_bound", PlatformServiceTests::commonTagsAreBound);
        out.accept("every_item_tag_has_a_name", PlatformServiceTests::everyItemTagHasAName);
    }

    /**
     * Every server-safe service resolves to this loader's implementation. A missing
     * {@code META-INF/services} entry is silent until the first caller crashes; the class-name
     * check also catches the wrong loader's jar winning the lookup.
     */
    private static void servicesAllResolve(GameTestHelper helper) {
        String platform = Services.PLATFORM.getPlatformName();
        helper.assertTrue(platform.equals("Forge") || platform.equals("Fabric"),
                "unexpected platform name '" + platform + "'; TestSupport#onNeoForge compares against this exact string");

        assertLoaded(helper, platform, "IPlatformHelper", Services.PLATFORM);
        assertLoaded(helper, platform, "IConfigHelper", Services.CONFIG);
        assertLoaded(helper, platform, "IRegistryFactory", Services.REGISTRY_FACTORY);
        assertLoaded(helper, platform, "INetworkHelper", Services.NETWORK);
        assertLoaded(helper, platform, "ILevelPropertyAccessor", Services.LEVEL_PROPERTIES);
        assertLoaded(helper, platform, "IEventHelper", Services.EVENTS);
        assertLoaded(helper, platform, "IFluidManager", Services.FLUIDS);
        assertLoaded(helper, platform, "IConditionHelper", Services.CONDITIONS);
        assertLoaded(helper, platform, "IIngredientHelper", Services.INGREDIENTS);
        assertLoaded(helper, platform, "IWorldGenHelper", Services.WORLD_GEN);
        assertLoaded(helper, platform, "IInventoryHelper", Services.INVENTORY);

        // The environment queries, against a dedicated server running out of a gradle workspace.
        helper.assertFalse(Services.PLATFORM.isProduction(), "a gradle run reported itself as a production environment");
        helper.assertTrue(Services.PLATFORM.getCurrentDistribution() == Dist.DEDICATED_SERVER,
                "a gametest server reported distribution " + Services.PLATFORM.getCurrentDistribution());
        helper.assertFalse(Services.PLATFORM.isPhysicalClient(), "a gametest server reported itself as a physical client");

        helper.assertTrue(Services.PLATFORM.isModLoaded("assortedlib"), "assortedlib is not loaded, in its own gametest");
        helper.assertTrue(Services.PLATFORM.isModLoaded("minecraft"), "minecraft is not reported as a loaded mod");
        helper.assertFalse(Services.PLATFORM.isModLoaded("assortedlib_not_a_real_mod"), "an unknown mod id reported as loaded");

        // getRandomDungeonEntity reads MonsterRoomFeature.MOBS through an accessor mixin, so this is
        // also the cheapest check that the mixin config applied on this loader.
        EntityType<?> dungeonMob = Services.PLATFORM.getRandomDungeonEntity(RandomSource.create(1234L));
        helper.assertTrue(dungeonMob != null, "getRandomDungeonEntity returned null");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(dungeonMob) != null,
                "getRandomDungeonEntity returned an unregistered entity type");

        helper.succeed();
    }

    /** {@link ILoaderRegistry} is a straight view of a vanilla registry now, and must behave like one. */
    private static void registryRoundTrips(GameTestHelper helper) {
        ILoaderRegistry<Block> blocks = Services.PLATFORM.getRegistry(Registries.BLOCK);
        ResourceLocation stoneId = ResourceLocation.withDefaultNamespace("stone");

        helper.assertTrue(blocks.getValue(stoneId).orElse(null) == Blocks.STONE,
                "the block registry did not hand back the same Blocks.STONE instance");
        helper.assertTrue(stoneId.equals(blocks.getRegistryName(Blocks.STONE)), "minecraft:stone did not round-trip its id");
        helper.assertTrue(blocks.containsKey(stoneId), "the block registry does not contain minecraft:stone");
        helper.assertTrue(blocks.contains(Blocks.STONE), "the block registry does not contain Blocks.STONE");

        ILoaderRegistry<Item> items = Services.PLATFORM.getRegistry(Registries.ITEM);
        helper.assertTrue(items.getValue(ResourceLocation.withDefaultNamespace("stick")).orElse(null) == Items.STICK,
                "the item registry did not hand back the same Items.STICK instance");
        helper.assertTrue(items.getValues().count() == BuiltInRegistries.ITEM.keySet().size(),
                "the wrapped item registry and the vanilla one disagree on size");

        // An unknown id is empty on both loaders, including in DEFAULTED registries. Blocks, items and
        // fluids each have a default (air, air, empty) that vanilla's Registry#getValue answers for an
        // id nobody registered; NeoForge used to pass that through, so a lookup of an absent optional
        // item quietly got air there and nothing on Fabric.
        ResourceLocation missing = ResourceLocation.fromNamespaceAndPath("assortedlib", "not_a_real_entry");
        helper.assertFalse(blocks.containsKey(missing), "the block registry claims to contain " + missing);
        helper.assertTrue(blocks.getValue(missing).isEmpty(), "the block registry answered " + missing + " with " + blocks.getValue(missing).orElse(null));
        helper.assertTrue(items.getValue(missing).isEmpty(), "the item registry answered " + missing + " with " + items.getValue(missing).orElse(null));
        helper.assertTrue(Services.PLATFORM.getRegistry(Registries.FLUID).getValue(missing).isEmpty(), "the fluid registry answered " + missing + " with a fluid");

        helper.succeed();
    }

    /**
     * {@code Tiers} and {@code TieredItem} are gone; a tier is expressed as the vanilla harvest level
     * a tool can actually reach. Anchored against {@code ItemStack#isCorrectToolForDrops} rather than
     * against a recomputed expectation, so the test cannot drift with the implementation.
     */
    private static void tieredToolAgreesWithVanilla(GameTestHelper helper) {
        assertPickaxeTier(helper, Items.WOODEN_PICKAXE);
        assertPickaxeTier(helper, Items.STONE_PICKAXE);
        assertPickaxeTier(helper, Items.IRON_PICKAXE);
        assertPickaxeTier(helper, Items.DIAMOND_PICKAXE);
        assertPickaxeTier(helper, Items.NETHERITE_PICKAXE);
        assertPickaxeTier(helper, Items.GOLDEN_PICKAXE);

        // Wrong tool type, right tier: the tag gate has to reject these outright.
        helper.assertFalse(Services.PLATFORM.isTieredTool(new ItemStack(Items.IRON_SWORD), IPlatformHelper.ToolTier.WOOD, IPlatformHelper.ToolType.PICKAXE),
                "an iron sword passed as a pickaxe");
        helper.assertFalse(Services.PLATFORM.isTieredTool(new ItemStack(Items.SHEARS), IPlatformHelper.ToolTier.WOOD, IPlatformHelper.ToolType.PICKAXE),
                "shears passed as a pickaxe");
        helper.assertFalse(Services.PLATFORM.isTieredTool(new ItemStack(Items.IRON_SHOVEL), IPlatformHelper.ToolTier.IRON, IPlatformHelper.ToolType.PICKAXE),
                "an iron shovel passed as a pickaxe");

        // Only the tag half is asserted for a non-pickaxe: harvestLevelOf() probes iron ore, diamond
        // ore and obsidian, and no shovel, axe or hoe is ever "correct for drops" on any of them, so
        // every one of them reads as tier WOOD whatever it is made of. Reported, not papered over -
        // the sole caller today (MachineUtil) asks about PICKAXE, so nothing is broken by it yet.
        helper.assertTrue(Services.PLATFORM.isTieredTool(new ItemStack(Items.IRON_SHOVEL), IPlatformHelper.ToolTier.WOOD, IPlatformHelper.ToolType.SHOVEL),
                "an iron shovel failed the shovel tag gate");
        helper.assertFalse(Services.PLATFORM.isTieredTool(new ItemStack(Items.IRON_PICKAXE), IPlatformHelper.ToolTier.WOOD, IPlatformHelper.ToolType.SHOVEL),
                "an iron pickaxe passed as a shovel");
        helper.assertFalse(Services.PLATFORM.isTieredTool(new ItemStack(Items.STICK), IPlatformHelper.ToolTier.WOOD, IPlatformHelper.ToolType.PICKAXE),
                "a stick passed as a pickaxe");

        helper.succeed();
    }

    /** {@link com.grim3212.assorted.lib.platform.services.ILevelPropertyAccessor} against the live level. */
    private static void levelPropertiesMatchLevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos stone = helper.absolutePos(WORK);
        BlockPos ice = helper.absolutePos(WORK.east());
        BlockPos torch = helper.absolutePos(WORK.east(2));
        BlockPos glass = helper.absolutePos(WORK.east(3));

        helper.setBlock(WORK, Blocks.STONE);
        helper.setBlock(WORK.east(), Blocks.ICE);
        helper.setBlock(WORK.east(2), Blocks.TORCH);
        helper.setBlock(WORK.east(3), Blocks.GLASS);

        helper.assertValueEqual(Services.LEVEL_PROPERTIES.getFriction(level, ice, null),
                Blocks.ICE.getFriction(), "ice friction");
        helper.assertValueEqual(Services.LEVEL_PROPERTIES.getFriction(level, stone, null),
                Blocks.STONE.getFriction(), "stone friction");

        // Vanilla's torch emits 14. Spelled out rather than read back off the block state, because
        // NeoForge deprecates the position-free BlockState#getLightEmission in favour of an extension
        // that does not exist in the jar this module compiles against.
        helper.assertValueEqual(Services.LEVEL_PROPERTIES.getLightEmission(level, torch), 14, "torch light emission");
        helper.assertValueEqual(Services.LEVEL_PROPERTIES.getLightEmission(level, stone), 0, "stone light emission");

        helper.assertTrue(Services.LEVEL_PROPERTIES.getSoundType(level, stone, null) == SoundType.STONE, "stone sound type");
        helper.assertTrue(Services.LEVEL_PROPERTIES.getSoundType(level, glass, null) == SoundType.GLASS, "glass sound type");

        helper.assertValueEqual(Services.LEVEL_PROPERTIES.getLightBlock(level, stone),
                Blocks.STONE.defaultBlockState().getLightBlock(level, stone), "stone light dampening");
        helper.assertFalse(Services.LEVEL_PROPERTIES.propagatesSkylightDown(level, stone), "skylight passes through stone");

        helper.assertTrue(Services.LEVEL_PROPERTIES.shouldCheckWeakPower(level, stone, Direction.UP),
                "stone is not treated as a redstone conductor");
        helper.assertFalse(Services.LEVEL_PROPERTIES.shouldCheckWeakPower(level, glass, Direction.UP),
                "glass is treated as a redstone conductor");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cloned = Services.LEVEL_PROPERTIES.getCloneItemStack(level.getBlockState(stone), level, stone, player);
        helper.assertTrue(cloned.is(Items.STONE), "picking stone produced " + cloned);

        helper.succeed();
    }

    /**
     * The conventional tag names {@link LibCommonTags} declares resolve to populated tags. Asserts
     * membership, not existence: on Fabric the library declares these tags itself, so a wrong name
     * only shows where NeoForge provides them.
     */
    private static void commonTagsAreBound(GameTestHelper helper) {
        assertItemTagHolds(helper, LibCommonTags.Items.GUNPOWDER, Items.GUNPOWDER);
        assertItemTagHolds(helper, LibCommonTags.Items.LEATHER, Items.LEATHER);
        assertItemTagHolds(helper, LibCommonTags.Items.SLIMEBALLS, Items.SLIME_BALL);
        assertItemTagHolds(helper, LibCommonTags.Items.STRING, Items.STRING);
        assertItemTagHolds(helper, LibCommonTags.Items.OBSIDIAN, Items.OBSIDIAN);
        assertItemTagHolds(helper, LibCommonTags.Items.STONE, Items.STONE);
        assertItemTagHolds(helper, LibCommonTags.Items.COBBLESTONE, Items.COBBLESTONE);
        assertItemTagHolds(helper, LibCommonTags.Items.GRAVEL, Items.GRAVEL);
        assertItemTagHolds(helper, LibCommonTags.Items.GLASS, Items.GLASS);

        assertBlockTagHolds(helper, LibCommonTags.Blocks.OBSIDIAN, Blocks.OBSIDIAN);
        assertBlockTagHolds(helper, LibCommonTags.Blocks.STONE, Blocks.STONE);
        assertBlockTagHolds(helper, LibCommonTags.Blocks.COBBLESTONE, Blocks.COBBLESTONE);
        assertBlockTagHolds(helper, LibCommonTags.Blocks.GRAVEL, Blocks.GRAVEL);
        assertBlockTagHolds(helper, LibCommonTags.Blocks.GLASS, Blocks.GLASS);

        helper.succeed();
    }

    /**
     * Every item tag outside minecraft has a {@code tag.item.<namespace>.<path>} name ('/' becomes
     * '.'), the check behind Fabric API's "Untranslated Item Tags detected". Both loaders name the
     * standard c: tags, so anything missing is one of ours.
     */
    private static void everyItemTagHasAName(GameTestHelper helper) {
        Language language = Language.getInstance();
        List<String> missing = helper.getLevel().registryAccess().registryOrThrow(Registries.ITEM).getTagNames()
                .map(TagKey::location)
                .filter(id -> !"minecraft".equals(id.getNamespace()))
                .map(id -> "tag.item." + id.getNamespace() + "." + id.getPath().replace('/', '.'))
                .filter(key -> !language.has(key))
                .sorted()
                .toList();
        helper.assertTrue(missing.isEmpty(), "item tags with no name in any lang file: " + missing);
        helper.succeed();
    }

    /**
     * A shovel, axe or hoe reads the tier of its material, as a pickaxe does. The tier used to be
     * probed with ores only a pickaxe can mine, so every other tool type read as wood.
     */
    private static void toolTiersReadTheSameForEveryToolType(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        expectTier(wrong, Items.IRON_SHOVEL, IPlatformHelper.ToolType.SHOVEL, IPlatformHelper.ToolTier.IRON, IPlatformHelper.ToolTier.DIAMOND);
        expectTier(wrong, Items.IRON_AXE, IPlatformHelper.ToolType.AXE, IPlatformHelper.ToolTier.IRON, IPlatformHelper.ToolTier.DIAMOND);
        expectTier(wrong, Items.IRON_HOE, IPlatformHelper.ToolType.HOE, IPlatformHelper.ToolTier.IRON, IPlatformHelper.ToolTier.DIAMOND);
        expectTier(wrong, Items.STONE_HOE, IPlatformHelper.ToolType.HOE, IPlatformHelper.ToolTier.STONE, IPlatformHelper.ToolTier.IRON);
        expectTier(wrong, Items.WOODEN_SHOVEL, IPlatformHelper.ToolType.SHOVEL, IPlatformHelper.ToolTier.WOOD, IPlatformHelper.ToolTier.STONE);
        expectTier(wrong, Items.DIAMOND_AXE, IPlatformHelper.ToolType.AXE, IPlatformHelper.ToolTier.DIAMOND, null);
        expectTier(wrong, Items.IRON_PICKAXE, IPlatformHelper.ToolType.PICKAXE, IPlatformHelper.ToolTier.IRON, IPlatformHelper.ToolTier.DIAMOND);
        helper.assertTrue(wrong.isEmpty(), String.join(", ", wrong));
        helper.succeed();
    }

    /** The item reaches {@code tier} as a {@code type}, and does not reach {@code above} when one is given. */
    private static void expectTier(List<String> wrong, Item item, IPlatformHelper.ToolType type, IPlatformHelper.ToolTier tier, IPlatformHelper.ToolTier above) {
        ItemStack stack = new ItemStack(item);
        if (!Services.PLATFORM.isTieredTool(stack, tier, type)) {
            wrong.add(BuiltInRegistries.ITEM.getKey(item) + " does not reach " + tier);
        }
        if (above != null && Services.PLATFORM.isTieredTool(stack, above, type)) {
            wrong.add(BuiltInRegistries.ITEM.getKey(item) + " reaches " + above);
        }
    }
}
