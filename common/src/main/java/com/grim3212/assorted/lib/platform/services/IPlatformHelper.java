package com.grim3212.assorted.lib.platform.services;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.core.component.DataComponentType;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.mixin.world.level.MonsterRoomFeatureAccessor;
import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.function.BiFunction;
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

    /**
     * Shows a data component's {@link TooltipProvider} lines on every stack carrying it, ahead of
     * vanilla's own component lines; otherwise vanilla only asks the components on its fixed list.
     * Call once from common init, after the type is registered. Fabric adds these lines on the
     * client only, NeoForge on both sides.
     */
    <T extends TooltipProvider> void showComponentTooltip(Supplier<DataComponentType<T>> type);

    void addReloadListener(Identifier identifier, PreparableReloadListener reloadListener);

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

    /** Mob#finalizeSpawn, which NeoForge deprecates in favour of its FinalizeSpawnEvent hook; Fabric has only the method. */
    @Nullable
    SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData);

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

    /**
     * Burn times are data-driven and resolved per level. NeoForge deprecates
     * {@code FuelValues#burnDuration} for an {@code ItemStack} extension that only exists in its
     * patched jar, so the vanilla lookup is the only one common can call.
     */
    @SuppressWarnings("deprecation")
    default int getFuelTime(Level level, ItemStack stack) {
        return level.fuelValues().burnDuration(stack);
    }

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
        // Copper sits between stone and iron, but vanilla ships no NEEDS_COPPER_TOOL tag, so it is
        // only distinguishable down to the stone level.
        COPPER(1),
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
