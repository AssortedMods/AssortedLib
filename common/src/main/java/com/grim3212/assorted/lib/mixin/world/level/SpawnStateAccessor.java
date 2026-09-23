package com.grim3212.assorted.lib.mixin.world.level;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The mob cap checks NaturalSpawner makes before a pack, for a spawn habit that defers to its category's cap. */
@Mixin(NaturalSpawner.SpawnState.class)
public interface SpawnStateAccessor {

    @Invoker("canSpawnForCategoryGlobal")
    boolean assortedlib_canSpawnForCategoryGlobal(MobCategory category);

    @Invoker("canSpawnForCategoryLocal")
    boolean assortedlib_canSpawnForCategoryLocal(MobCategory category, ChunkPos chunk);
}
