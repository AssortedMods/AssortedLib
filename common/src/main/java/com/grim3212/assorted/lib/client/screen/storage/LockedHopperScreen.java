package com.grim3212.assorted.lib.client.screen.storage;

import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import com.grim3212.assorted.lib.core.storage.hopper.LockedHopperContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** A hopper's menu on the generic container background that fits its number of columns. */
public class LockedHopperScreen extends AbstractContainerScreen<LockedHopperContainer> {

    private static final Identifier CHEST_GUI_TEXTURE_4_COLS = StorageScreenTextures.generic(4);
    private static final Identifier CHEST_GUI_TEXTURE_5_COLS = StorageScreenTextures.generic(5);
    private static final Identifier CHEST_GUI_TEXTURE_6_COLS = StorageScreenTextures.generic(6);
    private static final Identifier CHEST_GUI_TEXTURE_7_COLS = StorageScreenTextures.generic(7);
    private static final Identifier CHEST_GUI_TEXTURE_8_COLS = StorageScreenTextures.generic(8);
    private static final Identifier CHEST_GUI_TEXTURE_9_COLS = StorageScreenTextures.generic(9);

    private static final int TEXTURE_X_SIZE = 256;
    private static final int TEXTURE_Y_SIZE = 276;
    private static final int START_OF_PLAYER_INVENTORY_Y = 180;
    private static final int HEIGHT_OF_PLAYER_INVENTORY = 96;

    private final int xRows;
    private final Identifier inventoryTexture;

    public LockedHopperScreen(LockedHopperContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, DEFAULT_IMAGE_WIDTH, imageHeight(container.getStorageMaterial()));

        StorageMaterial storageMaterial = container.getStorageMaterial();
        this.xRows = storageMaterial == null ? 1 : storageMaterial.hopperXRows();

        int yCols = storageMaterial == null ? 5 : storageMaterial.hopperYCols();
        switch (yCols) {
            case (4):
                this.inventoryTexture = CHEST_GUI_TEXTURE_4_COLS;
                break;
            case (5):
                this.inventoryTexture = CHEST_GUI_TEXTURE_5_COLS;
                break;
            case (6):
                this.inventoryTexture = CHEST_GUI_TEXTURE_6_COLS;
                break;
            case (7):
                this.inventoryTexture = CHEST_GUI_TEXTURE_7_COLS;
                break;
            case (8):
                this.inventoryTexture = CHEST_GUI_TEXTURE_8_COLS;
                break;
            default:
                this.inventoryTexture = CHEST_GUI_TEXTURE_9_COLS;
                break;
        }
    }

    // imageHeight is final on AbstractContainerScreen now, so it has to be worked out before the
    // super call rather than assigned afterwards.
    private static int imageHeight(StorageMaterial storageMaterial) {
        return 114 + (storageMaterial == null ? 1 : storageMaterial.hopperXRows()) * 18;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);

        int i = this.leftPos;
        int j = this.topPos;

        graphics.blit(RenderPipelines.GUI_TEXTURED, this.inventoryTexture, i, j, 0.0F, 0.0F, this.imageWidth, this.xRows * 18 + 17, TEXTURE_X_SIZE, TEXTURE_Y_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.inventoryTexture, i, j + this.xRows * 18 + 17, 0.0F, START_OF_PLAYER_INVENTORY_Y, this.imageWidth, HEIGHT_OF_PLAYER_INVENTORY, TEXTURE_X_SIZE, TEXTURE_Y_SIZE);
    }
}
