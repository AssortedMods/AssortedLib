package com.grim3212.assorted.lib.mixin.world.inventory;

import com.grim3212.assorted.lib.events.AnvilUpdatedEvent;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    @Shadow
    private String itemName;

    @Shadow
    private int repairItemCountCost;

    public AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(menuType, i, inventory, containerLevelAccess);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void assortedlib_checkForResults(CallbackInfo ci) {
        ItemStack leftSlot = inputSlots.getItem(0);
        ItemStack rightSlot = inputSlots.getItem(1);
        int baseCost = leftSlot.getOrDefault(DataComponents.REPAIR_COST, 0) + (rightSlot.isEmpty() ? 0 : rightSlot.getOrDefault(DataComponents.REPAIR_COST, 0));

        final AnvilUpdatedEvent event = new AnvilUpdatedEvent(leftSlot, rightSlot, itemName, baseCost, this.player);
        Services.EVENTS.handleEvents(event);

        if (event.isCanceled()) {
            // Canceled so just return empty
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            ci.cancel();
        } else if (!event.getOutput().isEmpty()) {
            this.resultSlots.setItem(0, event.getOutput());
            this.cost.set(event.getCost());
            this.repairItemCountCost = event.getMaterialCost();
            ci.cancel();
        }
    }
}
