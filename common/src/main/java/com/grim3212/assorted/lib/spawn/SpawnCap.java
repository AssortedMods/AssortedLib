package com.grim3212.assorted.lib.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Optional;

/**
 * No more than {@code max} of the counted types within {@code range} blocks of a spot. The counted
 * set is what makes a category: seals and walruses sharing one tag share one cap. Left out, only
 * the habit's own type counts. {@code skip_persistent} leaves out mobs that never despawn, tame ones
 * and the like, so they do not hold a spot against a wild one.
 */
public record SpawnCap(int range, int max, Optional<EntrySet<EntityType<?>>> counted, boolean skipPersistent) {

    public static final Codec<SpawnCap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 256).optionalFieldOf("range", 64).forGetter(SpawnCap::range),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("max").forGetter(SpawnCap::max),
            EntrySet.codec(Registries.ENTITY_TYPE).optionalFieldOf("counted").forGetter(SpawnCap::counted),
            Codec.BOOL.optionalFieldOf("skip_persistent", false).forGetter(SpawnCap::skipPersistent)
    ).apply(instance, SpawnCap::new));

    public boolean counts(Entity entity, EntityType<?> own) {
        if (!(entity instanceof Mob mob)) {
            return false;
        }
        if (this.skipPersistent && (mob.isPersistenceRequired() || mob.requiresCustomPersistence())) {
            return false;
        }
        return this.counted.map(set -> set.contains(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(mob.getType()))).orElse(mob.getType() == own);
    }
}
