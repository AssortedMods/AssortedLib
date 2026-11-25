package com.grim3212.assorted.lib.client.manual;

import com.grim3212.assorted.lib.client.screen.LibGuiItemRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One side of the spread while it is being drawn, in coordinates local to its own box.
 * <p>
 * Rebuilt each frame and kept by the screen until the next, which is how a click knows what was
 * under the cursor without a page holding state.
 */
public final class ManualPageView {

    public static final int LINE_HEIGHT = 9;

    private final GuiGraphics graphics;
    private final Font font;
    private final ManualBookStyle style;
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int mouseX;
    private final int mouseY;

    /** In whole lines, not pixels, so no line is ever drawn half cut off. */
    private final int scrollLines;

    /** Lines that did not fit, which bounds how far the screen lets the reader scroll. */
    private int overflowLines;

    private ItemStack hoveredStack = ItemStack.EMPTY;
    private final List<Component> hoveredLines = new ArrayList<>();
    private boolean animationFrozen;

    public ManualPageView(GuiGraphics graphics, Font font, ManualBookStyle style,
                          int left, int top, int width, int height, int mouseX, int mouseY, int scrollLines) {
        this.graphics = graphics;
        this.font = font;
        this.style = style;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.scrollLines = scrollLines;
    }

    public GuiGraphics graphics() {
        return this.graphics;
    }

    public Font font() {
        return this.font;
    }

    public ManualBookStyle style() {
        return this.style;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int absoluteX(int localX) {
        return this.left + localX;
    }

    public int absoluteY(int localY) {
        return this.top + localY;
    }

    /** The stack the cursor was over when this view was last drawn, or empty. */
    public ItemStack hoveredStack() {
        return this.hoveredStack;
    }

    /** Wraps to the page width; returns the height used, for stacking blocks of text. */
    public int text(FormattedText text, int localY) {
        return this.text(text, localY, this.height - localY);
    }

    /** @param available room before the text would run into whatever sits below it */
    public int text(FormattedText text, int localY, int available) {
        return this.text(text, 0, localY, this.width, available, this.style.textColor());
    }

    public int text(FormattedText text, int localX, int localY, int wrapWidth, int available, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, wrapWidth);
        int capacity = Math.max(0, available / LINE_HEIGHT);

        // What did not fit is what the reader may scroll to.
        this.overflowLines = Math.max(this.overflowLines, lines.size() - capacity);

        int first = Math.min(this.scrollLines, Math.max(0, lines.size() - capacity));
        int shown = Math.min(capacity, lines.size() - first);

        for (int i = 0; i < shown; i++) {
            this.graphics.drawString(this.font, lines.get(first + i), this.absoluteX(localX),
                    this.absoluteY(localY + i * LINE_HEIGHT), color, false);
        }

        return shown * LINE_HEIGHT;
    }

    /** Lines out of sight, and so the scroll limit. */
    public int overflowLines() {
        return Math.max(0, this.overflowLines);
    }

    public int scrollLines() {
        return this.scrollLines;
    }

    public void centeredText(Component text, int localY, int color) {
        int x = this.absoluteX((this.width - this.font.width(text)) / 2);
        this.graphics.drawString(this.font, text, x, this.absoluteY(localY), color, false);
    }

    public void sprite(ResourceLocation sprite, int localX, int localY, int spriteWidth, int spriteHeight) {
        this.graphics.blitSprite(sprite, this.absoluteX(localX), this.absoluteY(localY), spriteWidth, spriteHeight);
    }

    /** A texture region drawn at its own size. */
    public void texture(ResourceLocation texture, int localX, int localY, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        this.graphics.blit(texture, this.absoluteX(localX), this.absoluteY(localY),
                (float) u, (float) v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    /**
     * The same with rounded corners. A blit is always a rectangle, so the outermost rows go down
     * narrower, inset by what a circle of that radius would cut away.
     */
    public void roundedTexture(ResourceLocation texture, int localX, int localY, int u, int v, int regionWidth, int regionHeight,
                               int textureWidth, int textureHeight, int radius) {
        int corner = Math.max(0, Math.min(radius, Math.min(regionWidth, regionHeight) / 2));
        if (corner == 0) {
            this.texture(texture, localX, localY, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
            return;
        }

        for (int row = 0; row < corner; row++) {
            int inset = cornerInset(corner, row);
            int width = regionWidth - inset * 2;
            this.texture(texture, localX + inset, localY + row, u + inset, v + row, width, 1, textureWidth, textureHeight);
            this.texture(texture, localX + inset, localY + regionHeight - 1 - row, u + inset, v + regionHeight - 1 - row,
                    width, 1, textureWidth, textureHeight);
        }

        this.texture(texture, localX, localY + corner, u, v + corner, regionWidth, regionHeight - corner * 2,
                textureWidth, textureHeight);
    }

    /** How far row {@code row} of a corner is inset, following the circle. */
    private static int cornerInset(int radius, int row) {
        return (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - Math.pow(radius - row, 2))));
    }

    /** Box given in the page's own coordinates. */
    public boolean isMouseOver(int localX, int localY, int boxWidth, int boxHeight) {
        int x = this.absoluteX(localX);
        int y = this.absoluteY(localY);
        return this.mouseX >= x && this.mouseY >= y && this.mouseX < x + boxWidth && this.mouseY < y + boxHeight;
    }

    /** Called by a page while the cursor is over something it cycles, so it can be read. */
    public void freezeAnimation() {
        this.animationFrozen = true;
    }

    public boolean animationFrozen() {
        return this.animationFrozen;
    }

    /** A 16x16 slot. Tracks the cursor, for the tooltip and for click to follow. */
    public void item(ItemStack stack, int localX, int localY) {
        this.item(stack, localX, localY, List.of());
    }

    /** @param notes extra tooltip lines, such as the tag this slot accepts */
    public void item(ItemStack stack, int localX, int localY, List<Component> notes) {
        if (stack.isEmpty()) {
            return;
        }

        int x = this.absoluteX(localX);
        int y = this.absoluteY(localY);
        this.graphics.renderItem(stack, x, y);
        this.graphics.renderItemDecorations(this.font, stack, x, y);
        this.trackHover(stack, notes, x, y, 16, 16);
    }

    /** {@code GuiGraphics#renderItem} is fixed at 16x16, so the pose is scaled around the corner. */
    public void largeItem(ItemStack stack, int localX, int localY, float scale) {
        if (stack.isEmpty()) {
            return;
        }

        int x = this.absoluteX(localX);
        int y = this.absoluteY(localY);

        this.graphics.pose().pushPose();
        this.graphics.pose().translate((float) x, (float) y, 0.0F);
        this.graphics.pose().scale(scale, scale, 1.0F);
        LibGuiItemRenderer.item(this.graphics, stack, ItemDisplayContext.GUI, null, 0, 0);
        this.graphics.pose().popPose();

        this.trackHover(stack, List.of(), x, y, Math.round(16 * scale), Math.round(16 * scale));
    }

    /** A tooltip over a box that is not an item. */
    public void tooltip(int localX, int localY, int boxWidth, int boxHeight, Component text) {
        if (this.isMouseOver(localX, localY, boxWidth, boxHeight)) {
            this.hoveredStack = ItemStack.EMPTY;
            this.hoveredLines.clear();
            this.hoveredLines.add(text);
        }
    }

    private void trackHover(ItemStack stack, List<Component> notes, int x, int y, int hoverWidth, int hoverHeight) {
        if (this.mouseX >= x && this.mouseY >= y && this.mouseX < x + hoverWidth && this.mouseY < y + hoverHeight) {
            this.hoveredStack = stack;
            this.hoveredLines.clear();
            this.hoveredLines.addAll(notes);
        }
    }

    /** Drawn last, so a tooltip is never covered by the other page. */
    public void drawHoveredTooltip() {
        if (this.hoveredStack.isEmpty()) {
            if (!this.hoveredLines.isEmpty()) {
                this.graphics.renderComponentTooltip(this.font, this.hoveredLines, this.mouseX, this.mouseY);
            }
            return;
        }

        if (this.hoveredLines.isEmpty()) {
            this.graphics.renderTooltip(this.font, this.hoveredStack, this.mouseX, this.mouseY);
            return;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(this.hoveredStack.getHoverName());
        lines.addAll(this.hoveredLines);
        this.graphics.renderComponentTooltip(this.font, lines, this.mouseX, this.mouseY);
    }
}
