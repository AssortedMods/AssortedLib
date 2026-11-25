package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.conditions.PartToggles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import com.grim3212.assorted.lib.core.item.IStackTooltipProvider;
import net.minecraft.world.item.crafting.RecipeType;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.component.DataComponentType;
import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import com.grim3212.assorted.lib.core.inventory.MenuData;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.platform.services.IPlatformHelper;
import com.grim3212.assorted.lib.registry.ILoaderRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
        ItemGroupEvents.modifyEntriesEvent(key).register(entries -> {
            entries.acceptAll(PartToggles.visible(displayStacks.get()));
        });
    }

    @Override
    public void registerCreativeTab(ResourceLocation id, Supplier<CreativeModeTab> tab) {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab.get());
    }

    // Fabric registries stay open through every mod's initializer, so this can register at once.
    @Override
    public void registerDataComponentType(ResourceLocation id, Supplier<? extends DataComponentType<?>> type) {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type.get());
    }

    @Override
    public void modifyDefaultComponents(Supplier<? extends ItemLike> item, Consumer<DataComponentMap.Builder> components) {
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(item.get().asItem(), components));
    }

    @Override
    public <T extends IStackTooltipProvider> void showComponentTooltip(Supplier<DataComponentType<T>> type) {
        // Fabric's tooltip event is raised by a client mixin only, so there is nothing to hook on a dedicated server.
        if (this.isPhysicalClient()) {
            FabricComponentTooltips.show(type.get());
        }
    }

    @Override
    public <T extends LivingEntity> void registerEntityAttributes(Supplier<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {
        FabricDefaultAttributeRegistry.register(type.get(), attributes.get());
    }

    @Override
    public void registerFlammable(Supplier<? extends Block> block, int igniteOdds, int burnOdds) {
        FlammableBlockRegistry.getDefaultInstance().add(block.get(), burnOdds, igniteOdds);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData groupData) {
        return mob.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public <T extends Mob> void registerSpawnPlacement(Supplier<EntityType<T>> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        SpawnPlacements.register(type.get(), placement, heightmap, predicate);
    }

    @Override
    public void addReloadListener(ResourceLocation identifier, PreparableReloadListener reloadListener) {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricIdentifiedReloadListener(identifier, reloadListener));
    }

    @Override
    public void registerFuel(Supplier<? extends ItemLike> item, int burnTime) {
        FuelRegistry.INSTANCE.add(item.get(), burnTime);
    }

    @Override
    public void registerCompostable(Supplier<? extends ItemLike> item, float chance) {
        CompostingChanceRegistry.INSTANCE.add(item.get(), chance);
    }

    @Override
    public int getFuelTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        if (stack.isEmpty()) {
            return 0;
        }
        Integer time = FuelRegistry.INSTANCE.get(stack.getItem());
        return time == null ? 0 : time;
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> builder, Block... blocks) {
        // The data fixer type is null for a modded block entity, as vanilla's own lookup answers for any
        // type it has no schema for.
        return BlockEntityType.Builder.<T>of(builder::apply, blocks).build(null);
    }

    @Override
    public <T extends AbstractContainerMenu, D> MenuType<T> createMenuType(MenuFactory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        MenuType<T> type = new ExtendedScreenHandlerType<>(factory::create, codec);
        MenuData.register(type, factory, codec);
        return type;
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(SimpleMenuFactory<T> factory) {
        return new MenuType<>(factory::create, FeatureFlags.VANILLA_SET);
    }

    /** Fabric writes the data with the codec its {@link ExtendedScreenHandlerType} carries. */
    private record DataMenuProvider<D>(IMenuDataProvider<D> provider) implements ExtendedScreenHandlerFactory<D> {
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
