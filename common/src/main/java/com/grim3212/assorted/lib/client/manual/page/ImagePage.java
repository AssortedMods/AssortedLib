package com.grim3212.assorted.lib.client.manual.page;

import com.grim3212.assorted.lib.client.manual.ManualPage;
import com.grim3212.assorted.lib.client.manual.ManualPageView;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * A picture scaled to the page width, with text under it.
 *
 * @param image full texture path, such as {@code mymod:textures/gui/manual/kiln.png}
 * @param width the texture's own size, which cannot be asked for at this point
 */
public record ImagePage(Optional<Component> title, ResourceLocation image, int width, int height, Optional<Component> text) implements ManualPage {

    public static final MapCodec<ImagePage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ManualTextCodecs.TRANSLATABLE.optionalFieldOf("title").forGetter(ImagePage::title),
            ResourceLocation.CODEC.fieldOf("image").forGetter(ImagePage::image),
            Codec.INT.fieldOf("width").forGetter(ImagePage::width),
            Codec.INT.fieldOf("height").forGetter(ImagePage::height),
            ManualTextCodecs.TRANSLATABLE.optionalFieldOf("text").forGetter(ImagePage::text)
    ).apply(instance, ImagePage::new));

    @Override
    public MapCodec<? extends ManualPage> codec() {
        return CODEC;
    }

    @Override
    public void render(ManualPageView view, int animationTick) {
        int drawWidth = Math.min(this.width, view.width());
        int drawHeight = this.height * drawWidth / Math.max(1, this.width);
        int x = (view.width() - drawWidth) / 2;

        // The overload taking a destination and a source size scales; the shorter one crops.
        view.graphics().blit(this.image, view.absoluteX(x), view.absoluteY(0), drawWidth, drawHeight,
                0.0F, 0.0F, this.width, this.height, this.width, this.height);

        this.text.ifPresent(body -> view.text(body, drawHeight + 6, view.height() - drawHeight - 6));
    }
}
