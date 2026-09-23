package com.grim3212.assorted.lib.spawn;

import com.grim3212.assorted.lib.LibConstants;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The {@link SpawnSite} types a habit's {@code site.type} may name. Lib's are {@code land}, {@code water}, {@code column} and {@code structure}. */
public final class SpawnSites {

    private SpawnSites() {
    }

    private static final Map<Identifier, MapCodec<? extends SpawnSite>> TYPES = new ConcurrentHashMap<>();

    public static final Identifier LAND = register("land", LandSite.CODEC);
    public static final Identifier WATER = register("water", WaterSite.CODEC);
    public static final Identifier COLUMN = register("column", ColumnSite.CODEC);
    public static final Identifier STRUCTURE = register("structure", StructureSite.CODEC);

    public static Identifier register(String name, MapCodec<? extends SpawnSite> codec) {
        return register(Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, name), codec);
    }

    public static Identifier register(Identifier name, MapCodec<? extends SpawnSite> codec) {
        if (TYPES.putIfAbsent(name, codec) != null) {
            throw new IllegalStateException("Spawn site type registered twice: " + name);
        }
        return name;
    }

    static MapCodec<? extends SpawnSite> codec(Identifier type) {
        MapCodec<? extends SpawnSite> codec = TYPES.get(type);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown spawn site type " + type);
        }
        return codec;
    }
}
