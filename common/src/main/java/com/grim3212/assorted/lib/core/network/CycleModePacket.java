package com.grim3212.assorted.lib.core.network;

import com.grim3212.assorted.lib.core.item.ISwitchModes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Asks the server to cycle the mode of the {@link ISwitchModes} item in one hand. */
public record CycleModePacket(InteractionHand hand) {

    public static CycleModePacket decode(FriendlyByteBuf buf) {
        return new CycleModePacket(buf.readEnum(InteractionHand.class));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
    }

    public static void handle(CycleModePacket packet, Player player) {
        ItemStack stack = player.getItemInHand(packet.hand);
        if (stack.getItem() instanceof ISwitchModes item) {
            player.setItemInHand(packet.hand, item.cycleMode(player, stack));
        }
    }
}
