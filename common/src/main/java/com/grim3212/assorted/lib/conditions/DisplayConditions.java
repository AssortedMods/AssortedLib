package com.grim3212.assorted.lib.conditions;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The condition types json can name, and the codec that reads a list of them.
 * <p>
 * Written the way a recipe's load conditions are:
 * <pre>
 * "conditions": [
 *   { "type": "assortedlib:mod_loaded", "mod": "jei" },
 *   { "type": "assortedlib:not", "value": { "type": "assortedlib:part_enabled", "part": "cage" } }
 * ]
 * </pre>
 * A mod with a question of its own registers a type for it rather than making one of these fit.
 */
public final class DisplayConditions {

    // Concurrent: NeoForge constructs mods in parallel and each registers its own.
    private static final Map<Identifier, MapCodec<? extends DisplayCondition>> BY_ID = new ConcurrentHashMap<>();
    private static final Map<MapCodec<? extends DisplayCondition>, Identifier> BY_CODEC = new ConcurrentHashMap<>();

    /** Dispatch resolves per use, since init order across mods is not promised. */
    public static final Codec<DisplayCondition> CODEC = Identifier.CODEC
            .dispatch("type", condition -> BY_CODEC.get(condition.codec()), DisplayConditions::codecOf);

    /**
     * The same, resolved on first use. A condition that holds other conditions is a nested class of
     * this one, and a nested class is initialised without initialising the class around it, so
     * reading {@link #CODEC} directly from one of their codecs can read it as null.
     */
    private static final Codec<DisplayCondition> LAZY_CODEC = Codec.lazyInitialized(() -> CODEC);

    public static final Codec<List<DisplayCondition>> LIST_CODEC = CODEC.listOf();

    private DisplayConditions() {
    }

    public static void register(Identifier id, MapCodec<? extends DisplayCondition> codec) {
        BY_ID.put(id, codec);
        BY_CODEC.put(codec, id);
    }

    private static MapCodec<? extends DisplayCondition> codecOf(Identifier id) {
        MapCodec<? extends DisplayCondition> codec = BY_ID.get(id);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown display condition: " + id);
        }
        return codec;
    }

    public static boolean isRegistered(Identifier id) {
        return BY_ID.containsKey(id);
    }

    /** Whether every one of them passes; an empty list is unconditional. */
    public static boolean allMatch(List<DisplayCondition> conditions) {
        for (DisplayCondition condition : conditions) {
            if (!condition.test()) {
                return false;
            }
        }

        return true;
    }

    /** The types the library itself provides. Called from client init, and from datagen. */
    public static void bootstrap() {
        register(id("part_enabled"), PartEnabled.CODEC);
        register(id("mod_loaded"), ModLoaded.CODEC);
        register(id("item_exists"), ItemExists.CODEC);
        register(id("block_exists"), BlockExists.CODEC);
        register(id("all_of"), AllOf.CODEC);
        register(id("any_of"), AnyOf.CODEC);
        register(id("not"), Not.CODEC);
        register(id("recipe_exists"), RecipeExists.CODEC);
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, name);
    }

    /** A part of a mod that its config can switch off; see {@link LibParts}. */
    public record PartEnabled(String part) implements DisplayCondition {

        public static final MapCodec<PartEnabled> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Codec.STRING.fieldOf("part").forGetter(PartEnabled::part))
                .apply(instance, PartEnabled::new));

        @Override
        public boolean test() {
            return LibParts.isEnabled(this.part);
        }

        @Override
        public MapCodec<? extends DisplayCondition> codec() {
            return CODEC;
        }
    }

    public record ModLoaded(String mod) implements DisplayCondition {

        public static final MapCodec<ModLoaded> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Codec.STRING.fieldOf("mod").forGetter(ModLoaded::mod))
                .apply(instance, ModLoaded::new));

        @Override
        public boolean test() {
            return Services.PLATFORM.isModLoaded(this.mod);
        }

        @Override
        public MapCodec<? extends DisplayCondition> codec() {
            return CODEC;
        }
    }

    /** For content that only makes sense when another mod's item is present. */
    public record ItemExists(Identifier item) implements DisplayCondition {

        public static final MapCodec<ItemExists> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Identifier.CODEC.fieldOf("item").forGetter(ItemExists::item))
                .apply(instance, ItemExists::new));

        @Override
        public boolean test() {
            return BuiltInRegistries.ITEM.containsKey(this.item);
        }

        @Override
        public MapCodec<? extends DisplayCondition> codec() {
            return CODEC;
        }
    }

    public record BlockExists(Identifier block) implements DisplayCondition {

        public static final MapCodec<BlockExists> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Identifier.CODEC.fieldOf("block").forGetter(BlockExists::block))
                .apply(instance, BlockExists::new));

        @Override
        public boolean test() {
            return BuiltInRegistries.BLOCK.containsKey(this.block);
        }

        @Override
        public MapCodec<? extends DisplayCondition> codec() {
            return CODEC;
        }
    }

    public record AllOf(List<DisplayCondition> values) implements DisplayCondition {

        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(LAZY_CODEC.listOf().fieldOf("values").forGetter(AllOf::values))
                .apply(instance, AllOf::new));

        @Override
        public boolean test() {
            return allMatch(this.values);
        }

        @Override
        public MapCodec<? extends DisplayCondition> codec() {
            return CODEC;
        }
    }

    /** True when any one of them is, and so false when given nothing. */
    public record AnyOf(List<DisplayCondition> values) implements DisplayCondition {

        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(LAZY_CODEC.listOf().fieldOf("values").forGetter(AnyOf::values))
                .apply(instance, AnyOf::new));

        @Override
        public boolean test() {
            for (DisplayCondition condition : this.values) {
                if (condition.test()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public MapCodec<? extends DisplayCondition> codec() {
            return CODEC;
        }
    }

    public record Not(DisplayCondition value) implements DisplayCondition {

        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(LAZY_CODEC.fieldOf("value").forGetter(Not::value))
                .apply(instance, Not::new));

        @Override
        public boolean test() {
            return !this.value.test();
        }

        @Override
        public MapCodec<? extends DisplayCondition> codec() {
            return CODEC;
        }
    }
}
