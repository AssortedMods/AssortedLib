package com.grim3212.assorted.lib.platform;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.core.component.DataComponentType;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import com.grim3212.assorted.lib.core.inventory.MenuData;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.platform.services.IPlatformHelper;
import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }


    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider) {
        if (provider instanceof IMenuDataProvider<?> withData) {
            player.openMenu(new DataMenuProvider<>(withData));
        } else {
            player.openMenu(provider);
        }
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isFakePlayer(Player player) {
        return false;
    }

    @Override
    public double getPlayerReachDistance(Player player) {
        return player.isCreative() ? 5.0F : 4.5F;
    }

    @Override
    public Dist getCurrentDistribution() {
        return switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT -> Dist.CLIENT;
            case SERVER -> Dist.DEDICATED_SERVER;
        };
    }

    @Override
    public boolean isPhysicalClient() {
        return this.getCurrentDistribution() == Dist.CLIENT;
    }

    @Override
    public boolean isProduction() {
        return !FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public <T> ILoaderRegistry<T> getRegistry(ResourceKey<? extends Registry<T>> key) {
        return FabricRegistryWrapper.getRegistry(key);
    }

    @Override
    public void modifyCreativeTab(ResourceKey<CreativeModeTab> key, Supplier<List<ItemStack>> displayStacks) {
        CreativeModeTabEvents.modifyOutputEvent(key).register(output -> {
            output.acceptAll(displayStacks.get());
        });
    }

    @Override
    public <T extends TooltipProvider> void showComponentTooltip(Supplier<DataComponentType<T>> type) {
        ItemComponentTooltipProviderRegistry.addFirst(type.get());
    }

    @Override
    public <T extends LivingEntity> void registerEntityAttributes(Supplier<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {
        FabricDefaultAttributeRegistry.register(type.get(), attributes.get());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        return mob.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public <T extends Mob> void registerSpawnPlacement(Supplier<EntityType<T>> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        SpawnPlacements.register(type.get(), placement, heightmap, predicate);
    }

    @Override
    public void addReloadListener(Identifier identifier, PreparableReloadListener reloadListener) {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(identifier, reloadListener);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> builder, Block... blocks) {
        return FabricBlockEntityTypeBuilder.create(builder::apply, blocks).build();
    }

    @Override
    public <T extends AbstractContainerMenu, D> MenuType<T> createMenuType(MenuFactory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        MenuType<T> type = new ExtendedMenuType<>(factory::create, codec);
        MenuData.register(type, factory, codec);
        return type;
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(SimpleMenuFactory<T> factory) {
        return new MenuType<>(factory::create, FeatureFlags.VANILLA_SET);
    }

    /** Fabric writes the data with the codec its {@link ExtendedMenuType} carries. */
    private record DataMenuProvider<D>(IMenuDataProvider<D> provider) implements ExtendedMenuProvider<D> {
        @Override
        public D getScreenOpeningData(ServerPlayer player) {
            return provider.getMenuData(player);
        }

        @Override
        public Component getDisplayName() {
            return provider.getDisplayName();
        }

        @Override
        public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
            return provider.createMenu(windowId, inventory, player);
        }
    }
}
