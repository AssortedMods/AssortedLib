package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.platform.ForgePlatformHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Flammable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** NeoForge's fire odds for the blocks a mod passed to {@code registerFlammable}, which Fabric sets at runtime. */
final class FlammableDataMapProvider extends DataMapProvider {

    private final String modId;
    private final List<ForgePlatformHelper.FlammableRegistration> flammables;

    FlammableDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId, List<ForgePlatformHelper.FlammableRegistration> flammables) {
        super(output, lookupProvider);
        this.modId = modId;
        this.flammables = flammables;
    }

    @Override
    protected void gather(HolderLookup.Provider registries) {
        Builder<Flammable, Block> builder = builder(NeoForgeDataMaps.FLAMMABLE);
        this.flammables.forEach(flammable -> builder.add(flammable.block().get().builtInRegistryHolder(), new Flammable(flammable.igniteOdds(), flammable.burnOdds()), false));
    }

    // Unique per mod, as a datagen run refuses two providers with one name.
    @Override
    public String getName() {
        return "Flammables: " + this.modId;
    }
}
