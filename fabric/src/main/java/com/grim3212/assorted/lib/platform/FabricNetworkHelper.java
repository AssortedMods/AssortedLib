package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.network.LibPayload;
import com.grim3212.assorted.lib.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every packet is a {@link CustomPacketPayload} registered through {@link PayloadTypeRegistry};
 * {@link LibPayload} wraps each message, building its {@link StreamCodec} from the handler's
 * encoder/decoder pair, so the wire format matches NeoForge's.
 */
public class FabricNetworkHelper implements INetworkHelper {

    private static final Map<Class<?>, Registration<?>> messageHandlers = new ConcurrentHashMap<>();
    private static final List<Registration<?>> clientMessageHandlers = new ArrayList<>();

    private record Registration<MSG>(MessageHandler<MSG> handler, CustomPacketPayload.Type<LibPayload<MSG>> type) {
    }

    @Override
    public <MSG> void register(MessageHandler<MSG> handler) {
        final CustomPacketPayload.Type<LibPayload<MSG>> type = LibPayload.type(handler.id());
        final StreamCodec<RegistryFriendlyByteBuf, LibPayload<MSG>> codec = LibPayload.codec(type, handler.encoder(), handler.decoder());
        final Registration<MSG> registration = new Registration<>(handler, type);

        messageHandlers.put(handler.messageType(), registration);

        if (handler.side() == MessageBoundSide.CLIENT) {
            // The payload has to be declared on both sides, but only the client can receive it, and
            // the receiver is registered later from the client entrypoint.
            PayloadTypeRegistry.playS2C().register(type, codec);
            clientMessageHandlers.add(registration);
            return;
        }

        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> context.server().execute(
                () -> handler.messageConsumer().accept(payload.message(), context.player())));
    }

    public static void initializeClientHandlers() {
        for (Registration<?> registration : clientMessageHandlers) {
            registerClientReceiver(registration);
        }
    }

    private static <MSG> void registerClientReceiver(Registration<MSG> registration) {
        ClientPlayNetworking.registerGlobalReceiver(registration.type(), (payload, context) -> context.client().execute(
                () -> registration.handler().messageConsumer().accept(payload.message(), context.player())));
    }

    @SuppressWarnings("unchecked")
    private static <MSG> Registration<MSG> registrationFor(MSG toSend) {
        return (Registration<MSG>) messageHandlers.get(toSend.getClass());
    }

    @Override
    public <MSG> void sendToNearby(Level world, BlockPos pos, MSG toSend) {
        if (world instanceof ServerLevel serverLevel) {
            final Registration<MSG> registration = registrationFor(toSend);
            final LibPayload<MSG> payload = new LibPayload<>(registration.type(), toSend);
            PlayerLookup.around(serverLevel, pos, 64.0D).forEach(p -> ServerPlayNetworking.send(p, payload));
        }
    }

    @Override
    public <MSG> void sendToNearby(Level world, Entity entity, MSG toSend) {
        sendToNearby(world, entity.blockPosition(), toSend);
    }

    @Override
    public <MSG> void sendTo(Player player, MSG toSend) {
        final Registration<MSG> registration = registrationFor(toSend);
        ServerPlayNetworking.send((ServerPlayer) player, new LibPayload<>(registration.type(), toSend));
    }

    @Override
    public <MSG> void sendToServer(MSG toSend) {
        final Registration<MSG> registration = registrationFor(toSend);
        ClientPlayNetworking.send(new LibPayload<>(registration.type(), toSend));
    }
}
