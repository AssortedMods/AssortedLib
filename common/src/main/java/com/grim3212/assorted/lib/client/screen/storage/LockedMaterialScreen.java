package com.grim3212.assorted.lib.client.screen.storage;

import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import com.grim3212.assorted.lib.core.storage.LockedMaterialContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The screen of a {@link LockedMaterialContainer}, nine to fourteen columns wide. */
public class LockedMaterialScreen extends AbstractContainerScreen<LockedMaterialContainer> {

    private static final int START_OF_PLAYER_INVENTORY_Y = 180;
    private static final int HEIGHT_OF_PLAYER_INVENTORY = 96;

    private final int xRows;
    private final int textureXSize;
    private final int textureYSize;
    private final ResourceLocation inventoryTexture;

    public LockedMaterialScreen(LockedMaterialContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);

        StorageMaterial storageMaterial = container.getStorageMaterial();
        this.xRows = storageMaterial == null ? 3 : storageMaterial.getXRows();

        int yCols = storageMaterial == null ? 9 : storageMaterial.getYCols();
        this.imageWidth = 14 + yCols * 18;
        this.imageHeight = 114 + this.xRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
        switch (yCols) {
            case (10):
                this.textureXSize = 256;
                this.textureYSize = 276;
                this.inventoryTexture = StorageScreenTextures.generic(10);
                break;
            case (11):
                this.textureXSize = 256;
                this.textureYSize = 276;
                this.inventoryTexture = StorageScreenTextures.generic(11);
                break;
            case (12):
                this.textureXSize = 256;
                this.textureYSize = 276;
                this.inventoryTexture = StorageScreenTextures.generic(12);
                break;
            case (13):
                this.textureXSize = 276;
                this.textureYSize = 276;
                this.inventoryTexture = StorageScreenTextures.generic(13);
                break;
            case (14):
                this.textureXSize = 296;
                this.textureYSize = 276;
                this.inventoryTexture = StorageScreenTextures.generic(14);
                break;
            default:
                this.textureXSize = 256;
                this.textureYSize = 276;
                this.inventoryTexture = StorageScreenTextures.generic(9);
                break;
        }
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

        graphics.blit(this.inventoryTexture, i, j, 0.0F, 0.0F, this.imageWidth, this.xRows * 18 + 17, this.textureXSize, this.textureYSize);
        graphics.blit(this.inventoryTexture, i, j + this.xRows * 18 + 17, 0.0F, START_OF_PLAYER_INVENTORY_Y, this.imageWidth, HEIGHT_OF_PLAYER_INVENTORY, this.textureXSize, this.textureYSize);
    }
}
