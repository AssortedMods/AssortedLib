package com.grim3212.assorted.lib.spawn;

import com.grim3212.assorted.lib.conditions.LibParts;
import com.grim3212.assorted.lib.mixin.world.level.SpawnStateAccessor;
import com.grim3212.assorted.lib.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One creature's habit, from {@code data/<ns>/spawn_habit/<name>.json}: how the {@link CreatureSpawner}
 * sets it down, and at {@code seed} odds a chunk, how a chunk is populated as it generates. The creature's
 * registered spawn placement still decides whether a spot will do; this only picks spots and paces them. A
 * pack is placed whole or not at all. See {@link SpawnHabitBuilder} for the defaults, and README for the format.
 */
public record SpawnHabit(EntityType<?> entity, Optional<String> part, int interval, double chance, Span distance, int tries, SpawnSite site,
                         Optional<EntrySet<Biome>> biomes, Optional<EntrySet<Biome>> notBiomes, Daylight daylight, Span group, int spread,
                         Optional<SpawnCap> cap, boolean mobCap, boolean persistent, double seed) {

    public static final Codec<SpawnHabit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(SpawnHabit::entity),
            Codec.STRING.optionalFieldOf("part").forGetter(SpawnHabit::part),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("interval", SpawnHabitBuilder.DEFAULT_INTERVAL).forGetter(SpawnHabit::interval),
            Codec.doubleRange(0.0D, 1.0D).optionalFieldOf("chance", 1.0D).forGetter(SpawnHabit::chance),
            Span.CODEC.optionalFieldOf("distance", SpawnHabitBuilder.DEFAULT_DISTANCE).forGetter(SpawnHabit::distance),
            Codec.intRange(1, 64).optionalFieldOf("tries", SpawnHabitBuilder.DEFAULT_TRIES).forGetter(SpawnHabit::tries),
            SpawnSite.CODEC.optionalFieldOf("site", LandSite.DEFAULT).forGetter(SpawnHabit::site),
            EntrySet.codec(Registries.BIOME).optionalFieldOf("biomes").forGetter(SpawnHabit::biomes),
            EntrySet.codec(Registries.BIOME).optionalFieldOf("not_biomes").forGetter(SpawnHabit::notBiomes),
            Daylight.CODEC.optionalFieldOf("daylight", Daylight.ANY).forGetter(SpawnHabit::daylight),
            Span.CODEC.optionalFieldOf("group", SpawnHabitBuilder.DEFAULT_GROUP).forGetter(SpawnHabit::group),
            Codec.intRange(0, 16).optionalFieldOf("spread", SpawnHabitBuilder.DEFAULT_SPREAD).forGetter(SpawnHabit::spread),
            SpawnCap.CODEC.optionalFieldOf("cap").forGetter(SpawnHabit::cap),
            Codec.BOOL.optionalFieldOf("mob_cap", false).forGetter(SpawnHabit::mobCap),
            Codec.BOOL.optionalFieldOf("persistent", false).forGetter(SpawnHabit::persistent),
            Codec.doubleRange(0.0D, 1.0D).optionalFieldOf("seed", 0.0D).forGetter(SpawnHabit::seed)
    ).apply(instance, SpawnHabit::new));

    /** Off while its part is, or if the part is one nobody registered. */
    public boolean isEnabled() {
        return this.part.map(part -> LibParts.isRegistered(part) && LibParts.isEnabled(part)).orElse(true);
    }

    /** Hostile creatures wait for vanilla's word that hostile mobs may spawn. */
    public boolean isFriendly() {
        return this.entity.getCategory().isFriendly();
    }

    /** One try near every player in the level, each at the habit's odds. */
    public void spawnNearPlayers(ServerLevel level) {
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && random.nextDouble() < this.chance) {
                this.spawnNear(level, player.blockPosition(), random);
            }
        }
    }

    /** Columns at the habit's distance from {@code origin}, until one takes a pack. How many were set down. */
    public int spawnNear(ServerLevelAccessor level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < this.tries; attempt++) {
            int spawned = this.spawnPackAt(level, origin.offset(this.distance.away(random), 0, this.distance.away(random)), random, null);
            if (spawned > 0) {
                return spawned;
            }
        }
        return 0;
    }

    /**
     * A pack somewhere in {@code chunk} as it generates, at the habit's seed odds, the way the biome lists populate new
     * terrain. Kept inside the chunk: a worldgen thread must not read its neighbours. How many were set down.
     */
    public int seedChunk(ServerLevelAccessor level, ChunkPos chunk, RandomSource random) {
        if (this.seed <= 0.0D || !this.site.seedsAtGeneration() || !this.isEnabled() || random.nextDouble() >= this.seed) {
            return 0;
        }
        for (int attempt = 0; attempt < this.tries; attempt++) {
            BlockPos column = new BlockPos(chunk.getMinBlockX() + random.nextInt(16), 0, chunk.getMinBlockZ() + random.nextInt(16));
            int spawned = this.spawnPackAt(level, column, random, chunk);
            if (spawned > 0) {
                return spawned;
            }
        }
        return 0;
    }

    /** A pack at the column {@code around}: how many were set down, none or a whole group. */
    public int spawnPackAt(ServerLevelAccessor level, BlockPos around, RandomSource random) {
        return this.spawnPackAt(level, around, random, null);
    }

    private int spawnPackAt(ServerLevelAccessor level, BlockPos around, RandomSource random, @Nullable ChunkPos within) {
        if (level instanceof ServerLevel server && !server.isPositionEntityTicking(around)) {
            return 0;
        }
        BlockPos first = this.site.find(level, around, random);
        if (first == null || !this.suits(level, first) || this.isCapped(level, first) || this.mobCap && !this.underMobCap(level, first)) {
            return 0;
        }
        // Not while a chunk generates, where vanilla makes no such check either.
        if (within == null && this.tooCloseToPlayer(level, first)) {
            return 0;
        }

        int wanted = this.group.sample(random);
        List<Mob> pack = new ArrayList<>(wanted);
        SpawnGroupData groupData = null;
        for (int attempt = 0; attempt < wanted * 2 && pack.size() < wanted; attempt++) {
            BlockPos pos = attempt == 0 ? first : this.site.find(level, this.scatter(first, random, within), random);
            if (pos == null || isTaken(pack, pos) || !this.fits(level, pos)
                    || !SpawnPlacements.isSpawnPositionOk(this.entity, level, pos) || !SpawnPlacements.checkSpawnRules(this.entity, level, EntitySpawnReason.NATURAL, pos, random)) {
                continue;
            }
            Entity created = this.entity.create(level.getLevel(), EntitySpawnReason.NATURAL);
            if (!(created instanceof Mob mob)) {
                if (created != null) {
                    created.discard();
                }
                break;
            }
            mob.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            if (!mob.checkSpawnRules(level, EntitySpawnReason.NATURAL) || !mob.checkSpawnObstruction(level)) {
                mob.discard();
                continue;
            }
            groupData = Services.PLATFORM.finalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, groupData);
            if (this.persistent) {
                mob.setPersistenceRequired();
            }
            pack.add(mob);
        }

        // Whole or not at all.
        if (pack.size() < this.group.min()) {
            pack.forEach(Entity::discard);
            return 0;
        }
        pack.forEach(level::addFreshEntityWithPassengers);
        return pack.size();
    }


    /**
     * NaturalSpawner's own rule: nothing spawns inside 24 blocks of the nearest player. Against the nearest, not the one
     * the roll was for, or a spot 40 blocks from one player could be two from another.
     */
    private boolean tooCloseToPlayer(ServerLevelAccessor level, BlockPos pos) {
        int keepAway = this.distance.min();
        if (keepAway <= 0) {
            return false;
        }
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        Player nearest = level.getLevel().getNearestPlayer(x, pos.getY(), z, -1.0D, false);
        return nearest != null && nearest.distanceToSqr(x, pos.getY(), z) < (double) keepAway * keepAway;
    }

    /**
     * The creature's own box against the blocks, the last thing NaturalSpawner asks. The placement only refuses a full
     * cube, so without this a mob is set down standing inside a stair, a slab or a fence.
     */
    private boolean fits(ServerLevelAccessor level, BlockPos pos) {
        return level.noCollision(this.entity.getSpawnAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
    }

    private boolean suits(ServerLevelAccessor level, BlockPos pos) {
        if (this.biomes.isPresent() || this.notBiomes.isPresent()) {
            Holder<Biome> biome = level.getBiome(pos);
            if (this.biomes.map(set -> !set.contains(biome)).orElse(false) || this.notBiomes.map(set -> set.contains(biome)).orElse(false)) {
                return false;
            }
        }
        return this.daylight.allows(level.getLevel());
    }

    /** A generating chunk has no entities to count; the seed odds are what bound it there. */
    private boolean isCapped(ServerLevelAccessor level, BlockPos pos) {
        if (this.cap.isEmpty()) {
            return false;
        }
        SpawnCap cap = this.cap.get();
        return level.getEntities((Entity) null, new AABB(pos).inflate(cap.range()), entity -> cap.counts(entity, this.entity)).size() >= cap.max();
    }

    /** Vanilla's own cap checks for the creature's category, as NaturalSpawner makes them before a pack. Not at generation, as vanilla does not; no state yet is no. */
    private boolean underMobCap(ServerLevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return true;
        }
        NaturalSpawner.SpawnState state = server.getChunkSource().getLastSpawnState();
        if (!(state instanceof SpawnStateAccessor caps)) {
            return false;
        }
        return caps.assortedlib_canSpawnForCategoryGlobal(this.entity.getCategory()) && caps.assortedlib_canSpawnForCategoryLocal(this.entity.getCategory(), ChunkPos.containing(pos));
    }

    private BlockPos scatter(BlockPos first, RandomSource random, @Nullable ChunkPos within) {
        int x = first.getX() + random.nextInt(this.spread * 2 + 1) - this.spread;
        int z = first.getZ() + random.nextInt(this.spread * 2 + 1) - this.spread;
        if (within != null) {
            x = Mth.clamp(x, within.getMinBlockX(), within.getMaxBlockX());
            z = Mth.clamp(z, within.getMinBlockZ(), within.getMaxBlockZ());
        }
        return new BlockPos(x, first.getY(), z);
    }

    /** The pack is not in the level yet, so checkSpawnObstruction cannot see its own members. */
    private static boolean isTaken(List<Mob> pack, BlockPos pos) {
        for (Mob mob : pack) {
            if (mob.blockPosition().equals(pos)) {
                return true;
            }
        }
        return false;
    }
}
