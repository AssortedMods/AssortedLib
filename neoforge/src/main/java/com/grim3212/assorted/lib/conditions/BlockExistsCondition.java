package com.grim3212.assorted.lib.conditions;

import com.grim3212.assorted.lib.LibConstants;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Conditions are codec based, so there is no {@code IConditionSerializer} inner class;
 * the {@link MapCodec} below is what identifies the condition on both sides and is registered
 * against {@code NeoForgeRegistries.CONDITION_SERIALIZERS} by {@link LibConditions}.
 */
public class BlockExistsCondition implements ICondition {

    public static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "block_exists");
    public static final MapCodec<BlockExistsCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(ResourceLocation.CODEC.fieldOf("block").forGetter(condition -> condition.block))
            .apply(instance, BlockExistsCondition::new));

    private final ResourceLocation block;

    public BlockExistsCondition(ResourceLocation block) {
        this.block = block;
    }

    public ResourceLocation getBlock() {
        return this.block;
    }

    @Override
    public boolean test(IContext context) {
        return BuiltInRegistries.BLOCK.containsKey(this.block);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "block_exists(\"" + this.block + "\")";
    }
}
