package com.grim3212.assorted.lib.spawn;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/** A {@link SpawnHabit} in Java, for datagen and tests. Every knob has the default the JSON does. */
public final class SpawnHabitBuilder {

    public static final int DEFAULT_INTERVAL = 1200;
    public static final Span DEFAULT_DISTANCE = new Span(24, 48);
    public static final int DEFAULT_TRIES = 4;
    public static final Span DEFAULT_GROUP = new Span(1, 1);
    public static final int DEFAULT_SPREAD = 4;

    private final Supplier<? extends EntityType<?>> entity;
    @Nullable
    private String part;
    private int interval = DEFAULT_INTERVAL;
    private double chance = 1.0D;
    private Span distance = DEFAULT_DISTANCE;
    private int tries = DEFAULT_TRIES;
    private SpawnSite site = LandSite.DEFAULT;
    @Nullable
    private EntrySet<Biome> biomes;
    @Nullable
    private EntrySet<Biome> notBiomes;
    private Daylight daylight = Daylight.ANY;
    private Span group = DEFAULT_GROUP;
    private int spread = DEFAULT_SPREAD;
    @Nullable
    private SpawnCap cap;
    private boolean mobCap;
    private boolean persistent;
    private double seed;

    private SpawnHabitBuilder(Supplier<? extends EntityType<?>> entity) {
        this.entity = entity;
    }

    public static SpawnHabitBuilder of(Supplier<? extends EntityType<?>> entity) {
        return new SpawnHabitBuilder(entity);
    }

    /** Off while the part is: a name given to {@code IConditionHelper#registerPartCondition}. */
    public SpawnHabitBuilder part(String part) {
        this.part = part;
        return this;
    }

    /** Ticks between tries, per level. */
    public SpawnHabitBuilder every(int ticks) {
        this.interval = ticks;
        return this;
    }

    /** The odds, 0 to 1, that a try near any one player goes ahead at all. */
    public SpawnHabitBuilder chance(double chance) {
        this.chance = chance;
        return this;
    }

    /**
     * How far from the player, each way, the columns are picked: 24 to 48 like a patrol. The minimum is also how near
     * the nearest player may be, vanilla's own rule being 24, so keep it there unless the creature should turn up underfoot.
     */
    public SpawnHabitBuilder distance(int min, int max) {
        this.distance = new Span(min, max);
        return this;
    }

    /** Columns tried, per player, before giving up until the next interval. */
    public SpawnHabitBuilder tries(int tries) {
        this.tries = tries;
        return this;
    }

    public SpawnHabitBuilder at(SpawnSite site) {
        this.site = site;
        return this;
    }

    public SpawnHabitBuilder onLand() {
        return this.at(LandSite.DEFAULT);
    }

    /** Afloat at the top of open water. */
    public SpawnHabitBuilder inWater() {
        return this.at(WaterSite.SURFACE);
    }

    /** That deep under the top of the water, and under ice too if {@code underIce}. */
    public SpawnHabitBuilder inWater(int minDepth, int maxDepth, boolean underIce) {
        return this.at(new WaterSite(new Span(minDepth, maxDepth), underIce));
    }

    /** Any height in the column, the surface or a cave, as vanilla's spawner picks. */
    public SpawnHabitBuilder inColumn() {
        return this.at(ColumnSite.ANYWHERE);
    }

    /** Under the surface block only. */
    public SpawnHabitBuilder underground() {
        return this.at(ColumnSite.UNDERGROUND);
    }

    public SpawnHabitBuilder inStructures(TagKey<Structure> structures) {
        return this.at(new StructureSite(EntrySet.of(structures)));
    }

    public SpawnHabitBuilder inBiomes(TagKey<Biome> biomes) {
        this.biomes = EntrySet.of(biomes);
        return this;
    }

    /** Never where the biome is in this tag, whatever {@link #inBiomes} says. */
    public SpawnHabitBuilder notInBiomes(TagKey<Biome> biomes) {
        this.notBiomes = EntrySet.of(biomes);
        return this;
    }

    public SpawnHabitBuilder during(Daylight daylight) {
        this.daylight = daylight;
        return this;
    }

    /** How many are set down together, within {@link #spread} blocks of the first. All of them or none. */
    public SpawnHabitBuilder group(int min, int max) {
        this.group = new Span(min, max);
        return this;
    }

    public SpawnHabitBuilder spread(int blocks) {
        this.spread = blocks;
        return this;
    }

    /** No more than {@code max} of this creature within {@code range} blocks of the spot. */
    public SpawnHabitBuilder cap(int range, int max) {
        this.cap = new SpawnCap(range, max, Optional.empty(), false);
        return this;
    }

    /** No more than {@code max} of the tagged types within {@code range}: one cap shared by all of them. */
    public SpawnHabitBuilder cap(int range, int max, TagKey<EntityType<?>> counted) {
        this.cap = new SpawnCap(range, max, Optional.of(EntrySet.of(counted)), false);
        return this;
    }

    /** As {@link #cap(int, int)}, not counting mobs that never despawn, tame ones and the like. */
    public SpawnHabitBuilder capOfWild(int range, int max) {
        this.cap = new SpawnCap(range, max, Optional.empty(), true);
        return this;
    }

    /** Only while vanilla's cap for the creature's category has room, as a biome spawn would: a farm animal among farm animals. */
    public SpawnHabitBuilder mobCap() {
        this.mobCap = true;
        return this;
    }

    /** Never despawns, as a cat spawned in a witch hut does not. */
    public SpawnHabitBuilder persistent() {
        this.persistent = true;
        return this;
    }

    /** The odds, 0 to 1, that a chunk gets a pack as it generates, the way the biome lists populate new terrain. */
    public SpawnHabitBuilder seed(double chancePerChunk) {
        this.seed = chancePerChunk;
        return this;
    }

    public SpawnHabit build() {
        return new SpawnHabit(this.entity.get(), Optional.ofNullable(this.part), this.interval, this.chance, this.distance, this.tries, this.site,
                Optional.ofNullable(this.biomes), Optional.ofNullable(this.notBiomes), this.daylight, this.group, this.spread, Optional.ofNullable(this.cap), this.mobCap, this.persistent, this.seed);
    }
}
