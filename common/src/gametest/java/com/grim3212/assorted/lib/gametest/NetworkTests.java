package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.platform.services.INetworkHelper;
import com.grim3212.assorted.lib.test.TestSupport;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Lib's own messages are all server-bound, so {@link #install()} registers a client-bound one for these
 * tests. It runs from the loader entry points, while the mod is still loading.
 */
final class NetworkTests {

    private static boolean installed;

    private NetworkTests() {
    }

    /** A client-bound message with nothing in it; the client never sees one in a gametest run. */
    record TestMessage() {

        static TestMessage decode(FriendlyByteBuf buf) {
            return new TestMessage();
        }

        void encode(FriendlyByteBuf buf) {
        }

        static void handle(TestMessage message, Player player) {
        }
    }

    /** NeoForge's {@code RegisterEvent} fires once per registry, and a payload may only be registered once. */
    static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        Services.NETWORK.register(new INetworkHelper.MessageHandler<>(Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "test_message"),
                TestMessage.class, TestMessage::encode, TestMessage::decode, TestMessage::handle, INetworkHelper.MessageBoundSide.CLIENT));
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("lib_messages_skip_players_without_the_channel", NetworkTests::messagesSkipPlayersWithoutTheChannel);
    }

    /**
     * A player whose connection never agreed the channel, as a gametest's player, is passed over:
     * NeoForge throws "may not be sent to the client" for such a send, which took down whatever server
     * code sent a mod's packet with one nearby.
     */
    private static void messagesSkipPlayersWithoutTheChannel(GameTestHelper helper) {
        ServerPlayer player = TestSupport.survivalPlayer(helper);
        TestMessage message = new TestMessage();
        Services.NETWORK.sendTo(player, message);
        Services.NETWORK.sendToNearby(helper.getLevel(), player.blockPosition(), message);
        Services.NETWORK.sendToNearby(helper.getLevel(), player, message);
        helper.succeed();
    }
}
