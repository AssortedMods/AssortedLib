package com.grim3212.assorted.lib.client.manual.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/** A button drawn as its sprite alone, with no vanilla button frame. */
public class ManualSpriteButton extends Button {

    private final ResourceLocation sprite;
    private final ResourceLocation highlightedSprite;
    private final boolean pageTurnSound;

    public ManualSpriteButton(int x, int y, int width, int height, ResourceLocation sprite, ResourceLocation highlightedSprite, Component narration, OnPress onPress, boolean pageTurnSound) {
        super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
        this.sprite = sprite;
        this.highlightedSprite = highlightedSprite;
        this.pageTurnSound = pageTurnSound;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        ResourceLocation drawn = this.isHoveredOrFocused() ? this.highlightedSprite : this.sprite;
        graphics.blitSprite(drawn, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(this.pageTurnSound ? SoundEvents.BOOK_PAGE_TURN : SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }
}
