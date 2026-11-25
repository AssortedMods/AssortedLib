package com.grim3212.assorted.lib.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Carries an arbitrary message object as a {@link CustomPacketPayload}, with the codec built from
 * the encoder/decoder pair {@code INetworkHelper.MessageHandler} carries, so messages need not
 * implement it themselves. Both loaders use this wrapper, so the wire format matches.
 *
 * @param type the payload type this message was registered under
 */
public record LibPayload<MSG>(CustomPacketPayload.Type<LibPayload<MSG>> type, MSG message) implements CustomPacketPayload {

    public static <MSG> CustomPacketPayload.Type<LibPayload<MSG>> type(ResourceLocation id) {
        return new CustomPacketPayload.Type<>(id);
    }

    /**
     * Builds the stream codec from a message's encoder and decoder. Encoders written against
     * {@link FriendlyByteBuf} keep working, since {@link RegistryFriendlyByteBuf} extends it.
     */
    public static <MSG> StreamCodec<RegistryFriendlyByteBuf, LibPayload<MSG>> codec(
            CustomPacketPayload.Type<LibPayload<MSG>> type,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder) {
        return StreamCodec.of(
                (buf, payload) -> encoder.accept(payload.message(), buf),
                buf -> new LibPayload<>(type, decoder.apply(buf))
        );
    }
}
