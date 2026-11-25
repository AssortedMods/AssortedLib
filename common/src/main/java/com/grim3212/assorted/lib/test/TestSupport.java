package com.grim3212.assorted.lib.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.mixin.test.TestHelperAccessor;
import com.grim3212.assorted.lib.mixin.server.ServerPlayerAccessor;
import com.grim3212.assorted.lib.platform.Services;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.grim3212.assorted.lib.core.item.IStackTooltipProvider;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

/**
 * Gametest helpers every Assorted mod's {@code <Mod>TestSupport} imports statically. Ships in the
 * library jar, like vanilla's own gametest framework, so a downstream mod's tests reach it without a
 * second artifact; nothing here registers a test or is loaded outside one.
 */
public final class TestSupport {

    private TestSupport() {
    }

    /** Keeps every test player's profile name distinct when a test places more than one. */
    private static final AtomicInteger PLAYERS = new AtomicInteger();

    /**
     * A real, fully joined survival player. None of vanilla's mock players will do for survival
     * behaviour: {@code makeMockServerPlayerInLevel} is deprecated for removal and forced to creative,
     * and {@code makeMockPlayer} / {@code makeMockServerPlayer} are never placed, so their connection
     * is null. The player is removed again when the test ends.
     */
    public static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "assorted-test-" + PLAYERS.incrementAndGet());
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        whenTestEnds(helper, () -> level.getServer().getPlayerList().remove(player));

        player.setGameMode(GameType.SURVIVAL);
        helper.assertFalse(player.isCreative(), "the test player is in creative, which changes every path under test");
        return player;
    }

    /** Runs {@code action} once the test has passed or failed; 1.21.1's helper has no hook of its own for it. */
    public static void whenTestEnds(GameTestHelper helper, Runnable action) {
        ((TestHelperAccessor) helper).assortedlib_getTestInfo().addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo info) {
            }

            @Override
            public void testPassed(GameTestInfo info, GameTestRunner runner) {
                action.run();
            }

            @Override
            public void testFailed(GameTestInfo info, GameTestRunner runner) {
                action.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo original, GameTestInfo rerun, GameTestRunner runner) {
            }
        });
    }

    /** {@link #survivalPlayer(GameTestHelper)} holding {@code held} in the main hand. */
    public static ServerPlayer survivalPlayer(GameTestHelper helper, ItemStack held) {
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    /** {@link #survivalPlayer(GameTestHelper, ItemStack)} who can be hurt, as a new player is immune to most damage for its first three seconds. */
    public static ServerPlayer mortalPlayer(GameTestHelper helper, ItemStack held) {
        ServerPlayer player = survivalPlayer(helper, held);
        ((ServerPlayerAccessor) player).assortedlib_setSpawnInvulnerableTime(0);
        return player;
    }

    /** How many of {@code item} are anywhere in the player's inventory. */
    public static int countInInventory(Player player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /** Stands {@code entity} on top of {@code rel}, within reach of anything nearby. */
    public static <T extends Entity> T stand(GameTestHelper helper, T entity, BlockPos rel) {
        Vec3 on = helper.absoluteVec(Vec3.atBottomCenterOf(rel.above()));
        entity.moveTo(on.x, on.y, on.z, 0.0F, 0.0F);
        return entity;
    }

    /** Puts {@code entity} at a spot in the air inside the test box, looking at the given pitch. */
    public static <T extends Entity> T hover(GameTestHelper helper, T entity, Vec3 relative, float xRot) {
        Vec3 at = helper.absoluteVec(relative);
        entity.moveTo(at.x, at.y, at.z, 0.0F, xRot);
        return entity;
    }

    /** A hit on the middle of the top face of an absolute {@code pos}. */
    public static BlockHitResult hitTop(BlockPos pos) {
        return hitSide(pos, Direction.UP);
    }

    /** A hit on the middle of one face of an absolute {@code pos}. */
    public static BlockHitResult hitSide(BlockPos pos, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5D), face, pos, false);
    }

    /**
     * A right click on the top face of an absolute {@code pos} with {@code stack}, through the game
     * mode - the path that fires the loader's use-block event, unlike {@code ItemStack#useOn}.
     */
    public static InteractionResult rightClick(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos pos) {
        return player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitTop(pos));
    }

    /** {@code ItemStack#useOn} against the top face of the relative {@code rel} with what is in the main hand. */
    public static InteractionResult useOnTopOf(GameTestHelper helper, ServerPlayer player, BlockPos rel) {
        BlockHitResult hit = hitTop(helper.absolutePos(rel));
        return player.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    /** The lines one component adds to a stack's tooltip, in order. */
    public static <T extends IStackTooltipProvider> List<Component> tooltipLines(GameTestHelper helper, ItemStack stack, DataComponentType<T> type) {
        List<Component> lines = new ArrayList<>();
        T value = stack.get(type);
        if (value != null) {
            value.addToTooltip(Item.TooltipContext.of(helper.getLevel()), lines::add, TooltipFlag.NORMAL, stack);
        }
        return lines;
    }

    /** The translation keys of the lines one component adds to a stack's tooltip, in order. */
    public static <T extends IStackTooltipProvider> List<String> tooltipKeys(GameTestHelper helper, ItemStack stack, DataComponentType<T> type) {
        return tooltipLines(helper, stack, type).stream().map(TestSupport::tooltipKey).toList();
    }

    /** The translation keys of a stack's whole tooltip, as the loader builds it on the server. */
    public static List<String> fullTooltipKeys(GameTestHelper helper, ItemStack stack) {
        return stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL).stream().map(TestSupport::tooltipKey).toList();
    }

    /**
     * A component's translation key, or its text when it is not translatable. A dedicated server
     * loads no mod language file, so a rendered string would only be the key anyway.
     */
    public static String tooltipKey(Component line) {
        return line.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : line.getString();
    }

    /** Whether this run is NeoForge, for the few statements that legitimately differ by loader. */
    public static boolean onNeoForge() {
        return "Forge".equals(Services.PLATFORM.getPlatformName());
    }

    /**
     * Saves a block entity and fails the test if the save threw. {@code ItemStack#save} throws on an
     * empty stack rather than writing it, and in game that exception takes the chunk save with it.
     */
    public static CompoundTag saveWithoutProblems(GameTestHelper helper, BlockEntity blockEntity, String what) {
        try {
            return blockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
        } catch (RuntimeException e) {
            throw new GameTestAssertException(what + " could not be saved: " + e);
        }
    }

    /**
     * Writes the block entity at {@code rel} out and reads it back exactly as a chunk save and load
     * would, and hands back the copy that came off the tag.
     */
    public static <T extends BlockEntity> T afterReload(GameTestHelper helper, BlockPos rel, Class<T> type) {
        ServerLevel level = helper.getLevel();
        BlockEntity original = helper.getBlockEntity(rel);
        helper.assertTrue(type.isInstance(original), "the block entity at " + rel + " is not a " + type.getSimpleName());
        CompoundTag saved = original.saveWithFullMetadata(level.registryAccess());
        BlockEntity reloaded = BlockEntity.loadStatic(helper.absolutePos(rel), helper.getBlockState(rel), saved, level.registryAccess());
        helper.assertTrue(type.isInstance(reloaded),
                "a " + type.getSimpleName() + " did not come back from a save and load round trip");
        return type.cast(reloaded);
    }

    /**
     * The result of the one crafting recipe that matches {@code input}, through the recipe manager,
     * so a recipe dropped by a failing load condition fails here.
     */
    public static ItemStack craft(GameTestHelper helper, CraftingInput input, String what) {
        Optional<RecipeHolder<CraftingRecipe>> found = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(found.isPresent(), "no crafting recipe matched " + what);
        return found.get().value().assemble(input, helper.getLevel().registryAccess());
    }

    /** Every variant object in a blockstate json: each "variants" entry and each multipart "apply". */
    public static List<JsonObject> blockstateVariants(JsonObject blockstate) {
        List<JsonObject> out = new ArrayList<>();
        if (blockstate.has("variants")) {
            for (var entry : blockstate.getAsJsonObject("variants").entrySet()) {
                addVariants(entry.getValue(), out);
            }
        }
        if (blockstate.has("multipart")) {
            for (JsonElement part : blockstate.getAsJsonArray("multipart")) {
                addVariants(part.getAsJsonObject().get("apply"), out);
            }
        }
        return out;
    }

    /**
     * Fails when a mod ships a JEI plugin that Fabric will never load. JEI finds a plugin by
     * annotation on NeoForge but by the {@code jei_mod_plugin} entrypoint on Fabric, so a plugin
     * that only carries the annotation works on one loader and is silently absent on the other -
     * which is how Grim Cuisine's three machines came to have JEI pages on NeoForge and none on
     * Fabric. Only meaningful on a Fabric run; on NeoForge there is no manifest to read and this
     * passes.
     * <p>
     * The manifest is picked by mod id rather than by taking the first on the classpath: a gametest
     * source set ships a {@code fabric.mod.json} of its own, and that one shadows the mod's.
     *
     * @param pluginClass the binary name of the {@code @JeiPlugin} class, e.g.
     *                    {@code com.grim3212.assorted.cuisine.compat.jei.JEIAssortedCuisine}
     */
    public static void assertJeiPluginIsRegistered(GameTestHelper helper, String modId, String pluginClass) {
        JsonObject manifest = null;

        try {
            Enumeration<URL> found = TestSupport.class.getClassLoader().getResources("fabric.mod.json");
            while (found.hasMoreElements()) {
                try (InputStream in = found.nextElement().openStream()) {
                    JsonObject candidate = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (candidate.has("id") && modId.equals(candidate.get("id").getAsString())) {
                        manifest = candidate;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new GameTestAssertException("could not read fabric.mod.json: " + e);
        }

        // Not a Fabric run, so there is nothing here to get wrong.
        if (manifest == null) {
            return;
        }

        JsonObject entrypoints = manifest.getAsJsonObject("entrypoints");
        boolean declared = entrypoints != null && entrypoints.has("jei_mod_plugin")
                && StreamSupport.stream(entrypoints.getAsJsonArray("jei_mod_plugin").spliterator(), false)
                .anyMatch(entry -> pluginClass.equals(entry.isJsonObject()
                        ? entry.getAsJsonObject().get("value").getAsString()
                        : entry.getAsString()));

        if (!declared) {
            helper.fail(pluginClass + " is not a jei_mod_plugin entrypoint in " + modId + "'s fabric.mod.json, so JEI"
                    + " never loads it on Fabric and the mod's pages are missing there while NeoForge shows them");
        }
    }

    private static void addVariants(JsonElement element, List<JsonObject> out) {
        if (element == null) {
            return;
        }
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(variant -> out.add(variant.getAsJsonObject()));
        } else {
            out.add(element.getAsJsonObject());
        }
    }
}
