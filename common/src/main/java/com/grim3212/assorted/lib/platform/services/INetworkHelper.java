package com.grim3212.assorted.lib.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface INetworkHelper {
    <MSG> void register(MessageHandler<MSG> handler);

    <MSG> void sendToNearby(Level world, BlockPos pos, MSG toSend);

    <MSG> void sendToNearby(Level world, Entity entity, MSG toSend);

    <MSG> void sendTo(Player playerMP, MSG toSend);

    <MSG> void sendToServer(MSG msg);

    record MessageHandler<MSG>(ResourceLocation id,
                               Class<MSG> messageType,
                               BiConsumer<MSG, FriendlyByteBuf> encoder,
                               Function<FriendlyByteBuf, MSG> decoder,
                               BiConsumer<MSG, Player> messageConsumer,
                               MessageBoundSide side) {
    }

    enum MessageBoundSide {
        CLIENT, SERVER
    }
}
