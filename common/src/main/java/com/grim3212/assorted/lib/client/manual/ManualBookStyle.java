package com.grim3212.assorted.lib.client.manual;

import com.grim3212.assorted.lib.LibConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * How the book is drawn, from {@code assets/assortedlib/manual/book.json}. Every field is optional
 * and falls back to {@link #DEFAULT}, so a pack writes down only what it changes.
 * <p>
 * Only the left page's box is given; the right page's is it reflected through {@link #width()}.
 */
public record ManualBookStyle(ResourceLocation texture, int textureWidth, int textureHeight, int width, int height,
                              int titleY, int contentX, int contentY, int contentWidth, int contentHeight,
                              int footerY, int controlsY, int entryHeight, Colors colors) {

    public static final ManualBookStyle DEFAULT = new ManualBookStyle(
            ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "textures/gui/manual.png"), 512, 256,
            384, 236,
            14, 15, 30, 152, 176,
            216, 222, 14,
            Colors.DEFAULT);

    public static final Codec<ManualBookStyle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("texture", DEFAULT.texture).forGetter(ManualBookStyle::texture),
            Codec.INT.optionalFieldOf("texture_width", DEFAULT.textureWidth).forGetter(ManualBookStyle::textureWidth),
            Codec.INT.optionalFieldOf("texture_height", DEFAULT.textureHeight).forGetter(ManualBookStyle::textureHeight),
            Codec.INT.optionalFieldOf("width", DEFAULT.width).forGetter(ManualBookStyle::width),
            Codec.INT.optionalFieldOf("height", DEFAULT.height).forGetter(ManualBookStyle::height),
            Codec.INT.optionalFieldOf("title_y", DEFAULT.titleY).forGetter(ManualBookStyle::titleY),
            Codec.INT.optionalFieldOf("content_x", DEFAULT.contentX).forGetter(ManualBookStyle::contentX),
            Codec.INT.optionalFieldOf("content_y", DEFAULT.contentY).forGetter(ManualBookStyle::contentY),
            Codec.INT.optionalFieldOf("content_width", DEFAULT.contentWidth).forGetter(ManualBookStyle::contentWidth),
            Codec.INT.optionalFieldOf("content_height", DEFAULT.contentHeight).forGetter(ManualBookStyle::contentHeight),
            Codec.INT.optionalFieldOf("footer_y", DEFAULT.footerY).forGetter(ManualBookStyle::footerY),
            Codec.INT.optionalFieldOf("controls_y", DEFAULT.controlsY).forGetter(ManualBookStyle::controlsY),
            Codec.INT.optionalFieldOf("entry_height", DEFAULT.entryHeight).forGetter(ManualBookStyle::entryHeight),
            Colors.CODEC.optionalFieldOf("colors", Colors.DEFAULT).forGetter(ManualBookStyle::colors)
    ).apply(instance, ManualBookStyle::new));

    /** @param side 0 for the left page, 1 for the right */
    public int contentX(int side) {
        return side == 0 ? this.contentX : this.width - this.contentX - this.contentWidth;
    }

    /** Rows a list fits down one page. */
    public int entriesPerPage() {
        return Math.max(1, this.contentHeight / Math.max(1, this.entryHeight));
    }

    public int textColor() {
        return this.colors.text();
    }

    public int titleColor() {
        return this.colors.title();
    }

    public int mutedTextColor() {
        return this.colors.muted();
    }

    public int hoveredTextColor() {
        return this.colors.hovered();
    }

    public int errorTextColor() {
        return this.colors.error();
    }

    /** Opaque: a colour with no alpha draws nothing. */
    public record Colors(int text, int title, int muted, int hovered, int error) {

        public static final Colors DEFAULT = new Colors(0xFF000000, 0xFF0026FF, 0xFF6B6154, 0xFF9A3412, 0xFFAA0000);

        /** Hex, {@code #RRGGBB} or {@code #AARRGGBB}; one given without alpha is made opaque. */
        private static final Codec<Integer> COLOR = Codec.STRING.comapFlatMap(Colors::read, Colors::write);

        public static final Codec<Colors> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                COLOR.optionalFieldOf("text", DEFAULT.text).forGetter(Colors::text),
                COLOR.optionalFieldOf("title", DEFAULT.title).forGetter(Colors::title),
                COLOR.optionalFieldOf("muted", DEFAULT.muted).forGetter(Colors::muted),
                COLOR.optionalFieldOf("hovered", DEFAULT.hovered).forGetter(Colors::hovered),
                COLOR.optionalFieldOf("error", DEFAULT.error).forGetter(Colors::error)
        ).apply(instance, Colors::new));

        private static DataResult<Integer> read(String value) {
            String digits = value.startsWith("#") ? value.substring(1) : value;
            if (digits.length() != 6 && digits.length() != 8) {
                return DataResult.error(() -> "Not a colour, expected #RRGGBB or #AARRGGBB: " + value);
            }

            try {
                long parsed = Long.parseLong(digits, 16);
                return DataResult.success((int) (digits.length() == 6 ? parsed | 0xFF000000L : parsed));
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "Not a colour, expected hex digits: " + value);
            }
        }

        private static String write(int color) {
            return String.format("#%08X", color);
        }
    }
}
