package com.grim3212.assorted.lib.spawn;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;

/** When a habit may set a creature down, by the sky's own light. */
public enum Daylight implements StringRepresentable {
    ANY("any"),
    DAY("day"),
    NIGHT("night");

    public static final Codec<Daylight> CODEC = StringRepresentable.fromEnum(Daylight::values);

    private final String name;

    Daylight(String name) {
        this.name = name;
    }

    public boolean allows(ServerLevel level) {
        return switch (this) {
            case ANY -> true;
            case DAY -> level.isBrightOutside();
            case NIGHT -> !level.isBrightOutside();
        };
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
