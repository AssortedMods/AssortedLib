package com.grim3212.assorted.lib.spawn;

import com.grim3212.assorted.lib.LibConstants;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The {@link SpawnSite} types a habit's {@code site.type} may name. Lib's are {@code land}, {@code water}, {@code column} and {@code structure}. */
public final class SpawnSites {

    private SpawnSites() {
    }

    private static final Map<ResourceLocation, MapCodec<? extends SpawnSite>> TYPES = new ConcurrentHashMap<>();

    public static final ResourceLocation LAND = register("land", LandSite.CODEC);
    public static final ResourceLocation WATER = register("water", WaterSite.CODEC);
    public static final ResourceLocation COLUMN = register("column", ColumnSite.CODEC);
    public static final ResourceLocation STRUCTURE = register("structure", StructureSite.CODEC);

    public static ResourceLocation register(String name, MapCodec<? extends SpawnSite> codec) {
        return register(ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, name), codec);
    }

    public static ResourceLocation register(ResourceLocation name, MapCodec<? extends SpawnSite> codec) {
        if (TYPES.putIfAbsent(name, codec) != null) {
            throw new IllegalStateException("Spawn site type registered twice: " + name);
        }
        return name;
    }

    static MapCodec<? extends SpawnSite> codec(ResourceLocation type) {
        MapCodec<? extends SpawnSite> codec = TYPES.get(type);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown spawn site type " + type);
        }
        return codec;
    }
}
