package com.grim3212.assorted.lib.mixin.world.level;

import com.grim3212.assorted.lib.spawn.SpawnHabits;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Seeds spawn habits into a chunk as it generates, right after vanilla's own creature packs, from the same
 * call and random: after, so vanilla's packs land where they always did for a seed. Both loaders reach this
 * through ChunkGenerator#spawnOriginalMobs, which already honours disableMobGeneration.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerSeedMixin {

    @Inject(method = "spawnMobsForChunkGeneration", at = @At("TAIL"))
    private static void assortedlib_seedSpawnHabits(ServerLevelAccessor level, Holder<Biome> biome, ChunkPos chunkPos, RandomSource random, CallbackInfo ci) {
        SpawnHabits.seedChunk(level, chunkPos, random);
    }
}
