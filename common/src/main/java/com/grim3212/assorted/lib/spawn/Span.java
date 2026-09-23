package com.grim3212.assorted.lib.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

/** An inclusive min to max, as a JSON object of the two. */
public record Span(int min, int max) {

    public static final Codec<Span> CODEC = RecordCodecBuilder.<Span>create(instance -> instance.group(
            Codec.INT.fieldOf("min").forGetter(Span::min),
            Codec.INT.fieldOf("max").forGetter(Span::max)
    ).apply(instance, Span::new)).validate(span -> span.min() >= 0 && span.min() <= span.max()
            ? DataResult.success(span)
            : DataResult.error(() -> "min must be 0 or more and no more than max: " + span));

    public int sample(RandomSource random) {
        return this.min + random.nextInt(this.max - this.min + 1);
    }

    /** That far off along one axis, either way. */
    public int away(RandomSource random) {
        int distance = this.sample(random);
        return random.nextBoolean() ? distance : -distance;
    }
}
