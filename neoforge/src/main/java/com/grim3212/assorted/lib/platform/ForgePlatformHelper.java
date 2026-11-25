package com.grim3212.assorted.lib.platform;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.event.EventHooks;
import com.grim3212.assorted.lib.core.item.IStackTooltipProvider;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.component.DataComponentType;
import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import com.grim3212.assorted.lib.core.inventory.MenuData;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.platform.services.IPlatformHelper;
import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
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
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }


    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider) {
        if (provider instanceof IMenuDataProvider<?> withData) {
            player.openMenu(new DataMenuProvider(withData, player));
        } else {
            player.openMenu(provider);
        }
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }


    @Override
    public boolean isProduction() {
        return FMLEnvironment.production;
    }

    @Override
    public boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }

    @Override
    public double getPlayerReachDistance(Player player) {
        // Reach is an attribute now, split into a block and an entity range.
        return player.blockInteractionRange();
    }

    @Override
    public Dist getCurrentDistribution() {
        return switch (FMLEnvironment.dist) {
            case CLIENT -> Dist.CLIENT;
            case DEDICATED_SERVER -> Dist.DEDICATED_SERVER;
        };
    }

    @Override
    public boolean isPhysicalClient() {
        return this.getCurrentDistribution() == Dist.CLIENT;
    }


    @Override
    public <T> ILoaderRegistry<T> getRegistry(ResourceKey<? extends Registry<T>> key) {
        return ForgeRegistryWrapper.getRegistry(key);
    }

    /**
     * Every contributor to a tab, not just the last one: more than one mod can add to the same
     * vanilla tab, and Fabric's side registers a listener per call rather than replacing. Concurrent
     * because mods are constructed in parallel.
     */
    public static final Map<ResourceKey<CreativeModeTab>, List<Supplier<List<ItemStack>>>> tabsToRegister = new ConcurrentHashMap<>();

    @Override
    public void modifyCreativeTab(ResourceKey<CreativeModeTab> key, Supplier<List<ItemStack>> displayStacks) {
        tabsToRegister.computeIfAbsent(key, tab -> new CopyOnWriteArrayList<>()).add(displayStacks);
    }

    // Registered on Lib's own bus by AssortedLibForge: the tab's namespace may be a mod that is not installed.
    public static final Map<ResourceLocation, Supplier<CreativeModeTab>> creativeTabsToRegister = new ConcurrentHashMap<>();

    @Override
    public void registerCreativeTab(ResourceLocation id, Supplier<CreativeModeTab> tab) {
        creativeTabsToRegister.putIfAbsent(id, tab);
    }

    public static final Map<ResourceLocation, Supplier<? extends DataComponentType<?>>> componentTypesToRegister = new ConcurrentHashMap<>();

    @Override
    public void registerDataComponentType(ResourceLocation id, Supplier<? extends DataComponentType<?>> type) {
        componentTypesToRegister.putIfAbsent(id, type);
    }

    // Concurrent because mods are constructed in parallel.
    public static final List<Supplier<? extends DataComponentType<? extends IStackTooltipProvider>>> componentTooltips = new CopyOnWriteArrayList<>();

    // Read by AssortedLibForge's ItemTooltipEvent listener, once the types exist.
    @Override
    public <T extends IStackTooltipProvider> void showComponentTooltip(Supplier<DataComponentType<T>> type) {
        componentTooltips.add(type);
    }

    // Applied from AssortedLibForge's ModifyDefaultComponentsEvent listener. Concurrent because mods are constructed in parallel.
    public static final List<Pair<Supplier<? extends ItemLike>, Consumer<DataComponentMap.Builder>>> defaultComponents = new CopyOnWriteArrayList<>();

    @Override
    public void modifyDefaultComponents(Supplier<? extends ItemLike> item, Consumer<DataComponentMap.Builder> components) {
        defaultComponents.add(Pair.of(item, components));
    }

    // Both handed to their mod bus events from AssortedLibForge. Concurrent because mods are
    // constructed in parallel.
    public static final List<AttributeRegistration<?>> attributesToRegister = new CopyOnWriteArrayList<>();
    public static final List<SpawnPlacementRegistration<?>> spawnPlacementsToRegister = new CopyOnWriteArrayList<>();
    public static final List<FlammableRegistration> flammablesToRegister = new CopyOnWriteArrayList<>();

    @Override
    public void registerFlammable(Supplier<? extends Block> block, int igniteOdds, int burnOdds) {
        flammablesToRegister.add(new FlammableRegistration(block, igniteOdds, burnOdds));
    }

    /** Written to NeoForge's flammable data map by {@code ForgeRegistryEntries}, as NeoForge reads the odds from it. */
    public record FlammableRegistration(Supplier<? extends Block> block, int igniteOdds, int burnOdds) {
    }

    public static final List<FuelRegistration> fuelsToRegister = new CopyOnWriteArrayList<>();
    public static final List<CompostableRegistration> compostablesToRegister = new CopyOnWriteArrayList<>();

    @Override
    public void registerFuel(Supplier<? extends ItemLike> item, int burnTime) {
        fuelsToRegister.add(new FuelRegistration(item, burnTime));
    }

    @Override
    public void registerCompostable(Supplier<? extends ItemLike> item, float chance) {
        compostablesToRegister.add(new CompostableRegistration(item, chance));
    }

    /** Written to NeoForge's {@code furnace_fuels} data map by {@code ForgeRegistryEntries}. */
    public record FuelRegistration(Supplier<? extends ItemLike> item, int burnTime) {
    }

    /** Written to NeoForge's {@code compostables} data map by {@code ForgeRegistryEntries}. */
    public record CompostableRegistration(Supplier<? extends ItemLike> item, float chance) {
    }

    @Override
    public int getFuelTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return stack.getBurnTime(recipeType);
    }

    @Override
    public <T extends LivingEntity> void registerEntityAttributes(Supplier<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {
        attributesToRegister.add(new AttributeRegistration<>(type, attributes));
    }

    public record AttributeRegistration<T extends LivingEntity>(Supplier<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {
        public void register(EntityAttributeCreationEvent event) {
            event.put(this.type.get(), this.attributes.get().build());
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData groupData) {
        return EventHooks.finalizeMobSpawn(mob, level, difficulty, reason, groupData);
    }

    @Override
    public <T extends Mob> void registerSpawnPlacement(Supplier<EntityType<T>> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        spawnPlacementsToRegister.add(new SpawnPlacementRegistration<>(type, placement, heightmap, predicate));
    }

    public record SpawnPlacementRegistration<T extends Mob>(Supplier<EntityType<T>> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        public void register(RegisterSpawnPlacementsEvent event) {
            event.register(this.type.get(), this.placement, this.heightmap, this.predicate, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    @Override
    public void addReloadListener(ResourceLocation identifier, PreparableReloadListener reloadListener) {
        // NeoForge's server reload listeners are not named; the identifier is Fabric's.
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(reloadListener));
    }

    // getRandomDungeonEntity is no longer overridden here. Forge's DungeonHooks is gone and there is
    // no modded dungeon mob list to consult, so IPlatformHelper's default - which reads vanilla's own
    // list off the monster room feature - is correct on both loaders.

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> builder, Block... blocks) {
        // No data fixer type: a mod's block entities have no vanilla schema to fix up against.
        return BlockEntityType.Builder.<T>of(builder::apply, blocks).build(null);
    }

    @Override
    public <T extends AbstractContainerMenu, D> MenuType<T> createMenuType(MenuFactory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        MenuType<T> type = IMenuTypeExtension.create((containerId, inventory, buf) -> factory.create(containerId, inventory, codec.decode(buf)));
        MenuData.register(type, factory, codec);
        return type;
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(SimpleMenuFactory<T> factory) {
        return new MenuType<>(factory::create, FeatureFlags.VANILLA_SET);
    }

    /**
     * NeoForge hands {@code writeClientSideData} the menu it just built, so the data is written with
     * that menu type's codec.
     */
    private record DataMenuProvider(IMenuDataProvider<?> provider, ServerPlayer player) implements MenuProvider {
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return provider.createMenu(containerId, inventory, player);
        }

        @Override
        public Component getDisplayName() {
            return provider.getDisplayName();
        }

        @Override
        public boolean shouldTriggerClientSideContainerClosingOnOpen() {
            return provider.shouldTriggerClientSideContainerClosingOnOpen();
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
            MenuData.write(menu.getType(), provider.getMenuData(player), buf);
        }
    }
}
