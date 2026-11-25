package com.grim3212.assorted.lib.conditions;

import com.grim3212.assorted.lib.LibConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;

public class PartEnabledCondition implements ICondition {

    public static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "part_enabled");
    public static final MapCodec<PartEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.STRING.fieldOf("part").forGetter(condition -> condition.part))
            .apply(instance, PartEnabledCondition::new));

    private final String part;

    public PartEnabledCondition(String part) {
        this.part = part;
    }

    public String getPart() {
        return this.part;
    }

    @Override
    public boolean test(IContext context) {
        if (!LibParts.isRegistered(this.part)) {
            throw new IllegalArgumentException("Can't check part that doesn't exist!");
        }

        return LibParts.isEnabled(this.part);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "part_enabled(\"" + this.part + "\")";
    }
}
