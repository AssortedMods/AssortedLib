package com.grim3212.assorted.lib.platform;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.event.EventHooks;
import java.util.ArrayList;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.core.component.DataComponentType;
import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import com.grim3212.assorted.lib.core.inventory.MenuData;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.platform.services.IPlatformHelper;
import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
        return FMLEnvironment.isProduction();
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
        return switch (FMLEnvironment.getDist()) {
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

    public static final List<Supplier<? extends DataComponentType<? extends TooltipProvider>>> componentTooltips = new ArrayList<>();

    // Registered from AssortedLibForge's RegisterTooltipAppendersEvent listener, once the types exist.
    @Override
    public <T extends TooltipProvider> void showComponentTooltip(Supplier<DataComponentType<T>> type) {
        componentTooltips.add(type);
    }

    // Both handed to their mod bus events from AssortedLibForge. Concurrent because mods are
    // constructed in parallel.
    public static final List<AttributeRegistration<?>> attributesToRegister = new CopyOnWriteArrayList<>();
    public static final List<SpawnPlacementRegistration<?>> spawnPlacementsToRegister = new CopyOnWriteArrayList<>();

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
    public @Nullable SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
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
    public void addReloadListener(Identifier identifier, PreparableReloadListener reloadListener) {
        // AddReloadListenerEvent was split per side and listeners are named now, so the identifier
        // this interface has always carried finally has a use.
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) -> event.addListener(identifier, reloadListener));
    }

    // getRandomDungeonEntity is no longer overridden here. Forge's DungeonHooks is gone and there is
    // no modded dungeon mob list to consult, so IPlatformHelper's default - which reads vanilla's own
    // list off the monster room feature - is correct on both loaders.

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> builder, Block... blocks) {
        // BlockEntityType.Builder is gone; the type takes its factory and valid blocks directly.
        return new BlockEntityType<>(builder::apply, blocks);
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
