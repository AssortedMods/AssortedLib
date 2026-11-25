package com.grim3212.assorted.lib.client.screen.storage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Base for the storage screens. It draws the background texture and the hovered slot's tooltip;
 * the base screen draws the labels and contents.
 */
public abstract class BaseStorageScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    private static final ResourceLocation CHEST_GUI_TEXTURE = ResourceLocation.parse("textures/gui/container/generic_54.png");
    protected final int inventoryRows;

    public BaseStorageScreen(T container, Inventory playerInventory, Component title) {
        this(container, playerInventory, title, 3);
    }

    public BaseStorageScreen(T container, Inventory playerInventory, Component title, int rows) {
        super(container, playerInventory, title);

        this.imageHeight = 114 + rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;

        this.inventoryRows = rows;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        graphics.blit(CHEST_GUI_TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.inventoryRows * 18 + 17, 256, 256);
        graphics.blit(CHEST_GUI_TEXTURE, i, j + this.inventoryRows * 18 + 17, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }
}
