package com.grim3212.assorted.lib.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grim3212.assorted.lib.LibConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Client bound config syncing: the whole config tree as one json string, in a
 * {@link CustomPacketPayload} registered through {@link PayloadTypeRegistry}.
 */
public class FabricConfigurationNetworkingUtils {

    /**
     * The channel the synced configs are sent over. Kept here rather than in the config helper
     * because the payload type has to be declared against a fixed id at class initialisation.
     */
    public static final ResourceLocation CONFIG_SYNC_CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "config_sync");

    private FabricConfigurationNetworkingUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: FabricConfigurationNetworkingUtils. This is a utility class");
    }

    /**
     * The synced configuration payload: the full config tree, serialised as one json object.
     */
    public record ConfigSyncPayload(String config) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ConfigSyncPayload> TYPE = new CustomPacketPayload.Type<>(CONFIG_SYNC_CHANNEL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(Integer.MAX_VALUE / 4), ConfigSyncPayload::config,
                ConfigSyncPayload::new
        );

        @Override
        public CustomPacketPayload.Type<ConfigSyncPayload> type() {
            return TYPE;
        }
    }

    /**
     * Declares the config sync payload on both sides. Has to run during mod initialisation, before
     * any connection is opened.
     */
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
    }

    /**
     * Registers the client side receiver that applies the synced configs. {@code channelName} is
     * only checked against {@link #CONFIG_SYNC_CHANNEL_ID}.
     */
    public static void registerNetworkingChannel(final ResourceLocation channelName, final Gson gson, Supplier<Map<String, FabricConfigurationSpec>> syncedSourcesProvider) {
        if (!CONFIG_SYNC_CHANNEL_ID.equals(channelName))
            throw new IllegalArgumentException("Only the " + CONFIG_SYNC_CHANNEL_ID + " channel is supported, got: " + channelName);

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
            final JsonElement jsonElement = gson.fromJson(payload.config(), JsonElement.class);
            if (!jsonElement.isJsonObject())
                throw new JsonParseException("The synced configs must be send in an object!");

            final JsonObject jsonObject = jsonElement.getAsJsonObject();

            syncedSourcesProvider.get().forEach((key, spec) -> {
                spec.reset();
                if (jsonObject.has(key)) {
                    final JsonElement specData = jsonObject.get(key);
                    if (!specData.isJsonObject())
                        throw new JsonParseException("A single synced config must be send in an object!");

                    spec.loadFrom(specData.getAsJsonObject());
                }
            });
        });
    }

    /** Sends the serialised config tree to one player. */
    public static void sendTo(final ServerPlayer serverPlayer, final String payload) {
        ServerPlayNetworking.send(serverPlayer, new ConfigSyncPayload(payload));
    }
}
