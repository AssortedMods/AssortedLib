package com.grim3212.assorted.lib.mixin.world.level;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The mob cap checks NaturalSpawner makes before a pack, for a spawn habit that defers to its category's cap. */
@Mixin(NaturalSpawner.SpawnState.class)
public interface SpawnStateAccessor {

    @Invoker("canSpawnForCategory")
    boolean assortedlib_canSpawnForCategory(MobCategory category, ChunkPos chunk);
}
