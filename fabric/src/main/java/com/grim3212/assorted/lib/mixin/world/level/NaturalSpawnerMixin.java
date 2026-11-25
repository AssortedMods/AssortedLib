package com.grim3212.assorted.lib.mixin.world.level;

import com.grim3212.assorted.lib.worldgen.StructureSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Appends {@link StructureSpawns} to what may spawn at a position. NeoForge fires
 * {@code LevelEvent.PotentialSpawns} from this same method; Fabric's API only reaches biomes, so the
 * result is extended here instead. Nothing vanilla allows is removed or reweighted.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
    private static void assortedlib_addStructureSpawns(ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory category, BlockPos pos, @Nullable Holder<Biome> biome,
                                                       CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        List<MobSpawnSettings.SpawnerData> added = StructureSpawns.at(structureManager, category, pos);
        if (!added.isEmpty()) {
            List<MobSpawnSettings.SpawnerData> spawns = new ArrayList<>(cir.getReturnValue().unwrap());
            spawns.addAll(added);
            cir.setReturnValue(WeightedRandomList.create(spawns));
        }
    }
}
