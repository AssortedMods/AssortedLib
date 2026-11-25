package com.grim3212.assorted.lib.platform.services;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import com.grim3212.assorted.lib.core.item.IStackTooltipProvider;
import net.minecraft.core.component.DataComponentType;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.mixin.world.level.MonsterRoomFeatureAccessor;
import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();


    /**
     * Opens a menu. When the provider is an {@link IMenuDataProvider}, its data is sent to the
     * client with the codec of the menu's type, which must come from
     * {@link #createMenuType(MenuFactory, StreamCodec)}. Any other provider opens as vanilla does.
     */
    void openMenu(ServerPlayer player, MenuProvider provider);

    /** Whether a mod with the given id is loaded. */
    boolean isModLoaded(String modId);

    boolean isFakePlayer(Player player);

    double getPlayerReachDistance(Player player);

    /**
     * The current distribution.
     *
     * @return The current distribution.
     */
    Dist getCurrentDistribution();

    boolean isPhysicalClient();

    /**
     * Indicates if the current platform is running in production mode or not.
     *
     * @return True if the current platform is running in production mode. False when not.
     */
    boolean isProduction();

    <T> ILoaderRegistry<T> getRegistry(ResourceKey<? extends Registry<T>> key);

    void modifyCreativeTab(final ResourceKey<CreativeModeTab> key, Supplier<List<ItemStack>> displayStacks);

    /** Registers a tab under an id no one mod owns, which is what a family's shared tab is; see {@code SharedCreativeTabs}. */
    void registerCreativeTab(ResourceLocation id, Supplier<CreativeModeTab> tab);

    /** Registers a data component type under an id no one mod owns; see {@code SharedDataComponents}. */
    void registerDataComponentType(ResourceLocation id, Supplier<? extends DataComponentType<?>> type);

    /**
     * Shows a data component's {@link IStackTooltipProvider} lines on every stack carrying it, ahead of
     * vanilla's own component lines; otherwise vanilla only asks the components on its fixed list.
     * Call once from common init, after the type is registered. Fabric adds these lines on the
     * client only, NeoForge on both sides.
     */
    <T extends IStackTooltipProvider> void showComponentTooltip(Supplier<DataComponentType<T>> type);

    /**
     * Sets components on an item made elsewhere. Call from mod construction, before the items are built; the item
     * is only asked for once they are.
     */
    void modifyDefaultComponents(Supplier<? extends ItemLike> item, Consumer<DataComponentMap.Builder> components);

    void addReloadListener(ResourceLocation identifier, PreparableReloadListener reloadListener);

    default EntityType<?> getRandomDungeonEntity(RandomSource random) {
        return Util.getRandom(MonsterRoomFeatureAccessor.getMOBS(), random);
    }

    /**
     * The attributes every entity of a living type starts with. A living type without them cannot be
     * spawned at all. Call from common init, after the type is registered.
     */
    <T extends LivingEntity> void registerEntityAttributes(Supplier<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes);

    /**
     * Where natural spawning may put a mob of this type, and the rule it has to pass there. Without
     * one, a type added to a biome's spawns is placed anywhere at all. Replaces any rule the type
     * already had. Call from common init, after the type is registered.
     */
    <T extends Mob> void registerSpawnPlacement(Supplier<EntityType<T>> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate);

    /**
     * Lets fire spread to and burn away a block, with vanilla's odds: planks are 5 and 20, logs 5 and 5.
     * Call from common init; on NeoForge the odds are data, written by {@code ForgeRegistryEntries}.
     */
    void registerFlammable(Supplier<? extends Block> block, int igniteOdds, int burnOdds);

    /**
     * Makes an item burn in a furnace for {@code burnTime} ticks; see {@code BurnTimes}. Call from mod construction;
     * on NeoForge burn times are data, the {@code neoforge:furnace_fuels} map written by {@code ForgeRegistryEntries}.
     */
    void registerFuel(Supplier<? extends ItemLike> item, int burnTime);

    /**
     * Makes an item compostable with the given chance of adding a layer; see {@code CompostChances}. Call from mod
     * construction; on NeoForge the chance is data, the {@code neoforge:compostables} map written by {@code ForgeRegistryEntries}.
     */
    void registerCompostable(Supplier<? extends ItemLike> item, float chance);

    /** Mob#finalizeSpawn, which NeoForge deprecates in favour of its FinalizeSpawnEvent hook; Fabric has only the method. */
    @Nullable
    SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData groupData);

    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> builder, Block... blocks);

    /**
     * A menu type whose client-side menu is built from data the server sends, encoded with
     * {@code codec}; open it from an {@link IMenuDataProvider}.
     */
    <T extends AbstractContainerMenu, D> MenuType<T> createMenuType(MenuFactory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec);

    /**
     * A menu type whose client-side menu needs nothing from the server; it opens as vanilla menus
     * do. Common code cannot build one itself: the {@code MenuType} constructor is only public once a
     * loader has widened it.
     */
    <T extends AbstractContainerMenu> MenuType<T> createMenuType(SimpleMenuFactory<T> factory);

    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu, D> {
        T create(int syncId, Inventory inventory, D data);
    }

    @FunctionalInterface
    interface SimpleMenuFactory<T extends AbstractContainerMenu> {
        T create(int syncId, Inventory inventory);
    }

    /** Whether a furnace would take this as fuel. Only reads the stack, so it is safe on the client. */
    default boolean isFuel(ItemStack stack) {
        return AbstractFurnaceBlockEntity.isFuel(stack);
    }

    /**
     * How long this burns in a furnace, 0 for anything that is not fuel: NeoForge's {@code IItemStackExtension#getBurnTime},
     * Fabric's {@code FuelRegistry}. Pass the recipe type a machine cooks, or null for a plain furnace's time; Fabric has
     * one time per item.
     */
    int getFuelTime(ItemStack stack, @Nullable RecipeType<?> recipeType);

    enum ToolType {
        PICKAXE(ItemTags.PICKAXES),
        SHOVEL(ItemTags.SHOVELS),
        AXE(ItemTags.AXES),
        HOE(ItemTags.HOES);

        private final TagKey<Item> tag;

        ToolType(TagKey<Item> tag) {
            this.tag = tag;
        }

        public TagKey<Item> getTag() {
            return this.tag;
        }
    }

    /**
     * A tool tier expressed as the vanilla harvest level it reaches, since tools only carry
     * per-block rules in {@link DataComponents#TOOL}.
     */
    enum ToolTier {
        WOOD(0),
        GOLD(0),
        STONE(1),
        IRON(2),
        // Netherite shares diamond's harvest level in vanilla; it differs in durability, not tier.
        DIAMOND(3),
        NETHERITE(3);

        private final int harvestLevel;

        ToolTier(int harvestLevel) {
            this.harvestLevel = harvestLevel;
        }

        public int getHarvestLevel() {
            return this.harvestLevel;
        }
    }

    default boolean isTieredTool(ItemStack stack, ToolTier minTier, ToolType toolType) {
        if (!stack.is(toolType.getTag())) {
            return false;
        }

        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        }

        return harvestLevelOf(tool) >= minTier.getHarvestLevel();
    }

    /**
     * Probes one representative block per vanilla harvest tier (NEEDS_DIAMOND/IRON/STONE_TOOL),
     * asking only the rules that deny drops. Those carry the material whatever the tool type, so a
     * shovel reads the same tier as a pickaxe of its material; asking whether it can mine the block
     * would not.
     */
    private static int harvestLevelOf(Tool tool) {
        if (!deniesDrops(tool, Blocks.OBSIDIAN.defaultBlockState())) {
            return 3;
        }
        if (!deniesDrops(tool, Blocks.DIAMOND_ORE.defaultBlockState())) {
            return 2;
        }
        if (!deniesDrops(tool, Blocks.IRON_ORE.defaultBlockState())) {
            return 1;
        }
        return 0;
    }

    private static boolean deniesDrops(Tool tool, BlockState state) {
        return tool.rules().stream().anyMatch(rule -> rule.correctForDrops().filter(correct -> !correct).isPresent()
                && rule.blocks().stream().anyMatch(block -> block.value() == state.getBlock()));
    }
}
