package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.platform.ForgePlatformHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * NeoForge's data maps for what a mod registered through {@code registerFuel} and {@code registerCompostable}:
 * NeoForge reads burn times and compost chances from them, where Fabric sets the same values at runtime.
 */
final class ForgeDataMapProvider extends DataMapProvider {

    private final String modId;
    private final List<ForgePlatformHelper.FuelRegistration> fuels;
    private final List<ForgePlatformHelper.CompostableRegistration> compostables;

    ForgeDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId,
                         List<ForgePlatformHelper.FuelRegistration> fuels,
                         List<ForgePlatformHelper.CompostableRegistration> compostables) {
        super(output, lookupProvider);
        this.modId = modId;
        this.fuels = fuels;
        this.compostables = compostables;
    }

    @Override
    protected void gather(HolderLookup.Provider registries) {
        if (!this.fuels.isEmpty()) {
            Builder<FurnaceFuel, Item> builder = builder(NeoForgeDataMaps.FURNACE_FUELS);
            this.fuels.forEach(fuel -> builder.add(BuiltInRegistries.ITEM.getKey(fuel.item().get().asItem()), new FurnaceFuel(fuel.burnTime()), false));
        }

        if (!this.compostables.isEmpty()) {
            Builder<Compostable, Item> builder = builder(NeoForgeDataMaps.COMPOSTABLES);
            this.compostables.forEach(compostable -> builder.add(BuiltInRegistries.ITEM.getKey(compostable.item().get().asItem()), new Compostable(compostable.chance()), false));
        }
    }

    // Unique per mod, as a datagen run refuses two providers with one name.
    @Override
    public String getName() {
        return "Data maps: " + this.modId;
    }
}
