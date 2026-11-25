package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.events.AnvilUpdatedEvent;
import com.grim3212.assorted.lib.events.CorrectToolForDropEvent;
import com.grim3212.assorted.lib.events.EntityInteractEvent;
import com.grim3212.assorted.lib.events.OnDropStacksEvent;
import com.grim3212.assorted.lib.events.UseBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.grim3212.assorted.lib.gametest.LibTestSupport.*;
import static com.grim3212.assorted.lib.test.TestSupport.survivalPlayer;

/**
 * The library's events reaching their handlers through the call sites each loader raises them from.
 */
final class EventTests {

    private EventTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("use_block_result_stops_vanilla", EventTests::useBlockResultStopsVanilla);
        out.accept("entity_interact_result_stops_vanilla", EventTests::entityInteractResultStopsVanilla);
        out.accept("anvil_event_sets_the_result", EventTests::anvilEventSetsTheResult);
        out.accept("drop_and_harvest_events_apply", EventTests::dropAndHarvestEventsApply);
    }

    /**
     * A {@code UseBlockEvent} handler that answers with a result ends the click on both loaders, so
     * vanilla's use of the block never runs: a stick used on a lever leaves it unflipped.
     */
    private static void useBlockResultStopsVanilla(GameTestHelper helper) {
        registerProbes();
        ServerLevel level = helper.getLevel();
        BlockPos lever = helper.absolutePos(WORK);
        helper.setBlock(WORK, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(lever), Direction.UP, lever, false);

        // The control: with nothing answering, the click reaches vanilla and flips the lever on.
        ItemStack plain = new ItemStack(Items.STICK);
        ServerPlayer player = survivalPlayer(helper, plain);
        player.gameMode.useItemOn(player, level, plain, InteractionHand.MAIN_HAND, hit);
        helper.assertBlockProperty(WORK, BlockStateProperties.POWERED, true);

        ItemStack probe = probe(Items.STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, probe);
        InteractionResult result = player.gameMode.useItemOn(player, level, probe, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(result.consumesAction(), "the handler's SUCCESS came back from the click as " + result);
        helper.assertBlockProperty(WORK, BlockStateProperties.POWERED, true);

        helper.succeed();
    }

    /**
     * An {@code EntityInteractEvent} handler that cancels with a result ends the interaction on
     * both loaders, as Tools' milking relies on: an empty bucket used on a cow stays empty.
     */
    private static void entityInteractResultStopsVanilla(GameTestHelper helper) {
        registerProbes();
        Cow cow = helper.spawn(EntityType.COW, WORK);

        ServerPlayer player = survivalPlayer(helper, new ItemStack(Items.BUCKET));
        player.interactOn(cow, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.MILK_BUCKET),
                "the control bucket was not milked, so the interaction never reached vanilla");

        player.setItemInHand(InteractionHand.MAIN_HAND, probe(Items.BUCKET));
        InteractionResult result = player.interactOn(cow, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction(), "the handler's SUCCESS came back from the interaction as " + result);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET),
                "the cow was milked after a handler had taken the interaction");

        helper.succeed();
    }

    /**
     * An {@code AnvilUpdatedEvent} handler's output lands in a real anvil's result slot: NeoForge's
     * {@code AnvilUpdateEvent} on one loader, the library's {@code AnvilMenuMixin} on the other.
     * Two sticks make nothing in vanilla, so any result at all came from the handler.
     */
    private static void anvilEventSetsTheResult(GameTestHelper helper) {
        registerProbes();
        ServerPlayer player = survivalPlayer(helper, ItemStack.EMPTY);

        helper.assertTrue(combine(player, new ItemStack(Items.STICK)).isEmpty(), "two plain sticks made something in an anvil");

        ItemStack result = combine(player, probe(Items.STICK));
        helper.assertTrue(result.is(Items.DIAMOND), "the handler's anvil output did not reach the result slot, got " + result);

        helper.succeed();
    }

    /**
     * {@code CorrectToolForDropEvent} and {@code OnDropStacksEvent} are raised by the library's own
     * common mixins rather than by a loader event, on both loaders; their answers must reach
     * {@code ItemStack#isCorrectToolForDrops} and {@code Block#getDrops}.
     */
    private static void dropAndHarvestEventsApply(GameTestHelper helper) {
        registerProbes();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        helper.assertFalse(new ItemStack(Items.STICK).isCorrectToolForDrops(obsidian), "a plain stick harvests obsidian");
        helper.assertTrue(probe(Items.STICK).isCorrectToolForDrops(obsidian), "the handler's answer did not reach ItemStack#isCorrectToolForDrops");

        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(WORK);
        BlockState stone = Blocks.STONE.defaultBlockState();

        List<ItemStack> plainDrops = Block.getDrops(stone, level, pos, null, null, new ItemStack(Items.STICK));
        helper.assertFalse(plainDrops.stream().anyMatch(drop -> drop.is(Items.DIAMOND)), "stone dropped a diamond without the handler");

        List<ItemStack> probeDrops = Block.getDrops(stone, level, pos, null, null, probe(Items.STICK));
        helper.assertTrue(probeDrops.size() == 1 && probeDrops.get(0).is(Items.DIAMOND), "the handler's drops did not reach Block#getDrops, got " + probeDrops);

        helper.succeed();
    }
}
