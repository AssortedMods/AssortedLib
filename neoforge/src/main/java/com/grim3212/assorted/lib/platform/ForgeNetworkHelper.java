package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.network.LibPayload;
import com.grim3212.assorted.lib.platform.services.INetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers each message as a {@link LibPayload}, whose {@link StreamCodec} is built from the
 * {@link MessageHandler}'s encoder/decoder pair; Fabric wraps them the same way, so the wire format
 * matches.
 */
public class ForgeNetworkHelper implements INetworkHelper {

    private static final String PROTOCOL = "7";

    private static final Map<Class<?>, Registration<?>> messageHandlers = new ConcurrentHashMap<>();
    private static final Map<String, Registrations> registrationsMap = new ConcurrentHashMap<>();

    @Override
    public <MSG> void register(MessageHandler<MSG> handler) {
        final CustomPacketPayload.Type<LibPayload<MSG>> type = LibPayload.type(handler.id());
        final Registration<MSG> registration = new Registration<>(handler, type, LibPayload.codec(type, handler.encoder(), handler.decoder()));

        messageHandlers.put(handler.messageType(), registration);
        getRegistrations().add(registration);
    }

    @Override
    public <MSG> void sendToNearby(Level world, BlockPos pos, MSG toSend) {
        if (world instanceof ServerLevel serverLevel) {
            // PacketDistributor.sendToPlayersNear throws on the first player whose connection lacks the
            // channel ("may not be sent to the client"), such as a gametest's mock player, so the players
            // in range are picked here, as vanilla's broadcast picks them, and checked one by one.
            final LibPayload<MSG> payload = payload(toSend);
            final double rangeSqr = 64.0D * 64.0D;
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < rangeSqr && player.connection.hasChannel(payload)) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
    }

    @Override
    public <MSG> void sendToNearby(Level world, Entity entity, MSG toSend) {
        sendToNearby(world, entity.blockPosition(), toSend);
    }

    @Override
    public <MSG> void sendTo(Player player, MSG toSend) {
        final LibPayload<MSG> payload = payload(toSend);
        final ServerPlayer serverPlayer = (ServerPlayer) player;
        if (serverPlayer.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
    }

    @Override
    public <MSG> void sendToServer(MSG toSend) {
        ClientPacketDistributor.sendToServer(payload(toSend));
    }

    @SuppressWarnings("unchecked")
    private static <MSG> LibPayload<MSG> payload(MSG message) {
        final Registration<MSG> registration = (Registration<MSG>) messageHandlers.get(message.getClass());
        if (registration == null) {
            throw new IllegalArgumentException("Tried to send an unregistered message: " + message.getClass().getName());
        }

        return new LibPayload<>(registration.type(), message);
    }

    /**
     * Payloads may only be registered from the mod event bus, so every mod that registers a message
     * gets one listener holding all of its registrations. The mod is the one currently being
     * constructed, which is what {@code ForgeClientHelper} keys its own registrations on too.
     */
    private static Registrations getRegistrations() {
        final var container = ModLoadingContext.get().getActiveContainer();
        return registrationsMap.computeIfAbsent(container.getModId(), modId -> {
            final Registrations newRegistrations = new Registrations();
            container.getEventBus().register(newRegistrations);
            return newRegistrations;
        });
    }

    public static class Registrations {
        private final List<Registration<?>> registrations = new ArrayList<>();

        private void add(Registration<?> registration) {
            this.registrations.add(registration);
        }

        @SubscribeEvent
        public void registerPayloads(final RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar(PROTOCOL);
            for (Registration<?> registration : this.registrations) {
                registration.register(registrar);
            }
        }
    }

    private record Registration<MSG>(MessageHandler<MSG> handler,
                                     CustomPacketPayload.Type<LibPayload<MSG>> type,
                                     StreamCodec<RegistryFriendlyByteBuf, LibPayload<MSG>> codec) {

        private void register(PayloadRegistrar registrar) {
            // The registrar already hands handlers to the main thread, and it registers the payload
            // for one direction only, so the explicit side check the old channel needed is gone.
            final IPayloadHandler<LibPayload<MSG>> payloadHandler = (payload, context) -> this.handler.messageConsumer().accept(payload.message(), context.player());

            if (this.handler.side() == MessageBoundSide.CLIENT) {
                registrar.playToClient(this.type, this.codec, payloadHandler);
            } else {
                registrar.playToServer(this.type, this.codec, payloadHandler);
            }
        }
    }
}
