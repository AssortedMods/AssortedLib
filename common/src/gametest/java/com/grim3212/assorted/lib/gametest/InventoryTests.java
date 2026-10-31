package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.impl.ItemStackStorageHandler;
import com.grim3212.assorted.lib.core.inventory.locking.LockSlot;
import com.grim3212.assorted.lib.core.inventory.slot.SlotStorageHandler;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.grim3212.assorted.lib.gametest.LibTestSupport.*;

/**
 * The library's item storage handler, and a block entity's inventory reached through the level.
 */
final class InventoryTests {

    private InventoryTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("storage_handler_insert_extract", InventoryTests::storageHandlerInsertExtract);
        out.accept("block_entity_handler_from_level", InventoryTests::blockEntityHandlerFromLevel);
        out.accept("storage_slots_share_one_container", InventoryTests::storageSlotsShareOneContainer);
    }

    /**
     * The library's own handler: slot limits, {@code isItemValid}, and simulation leaving the
     * inventory alone. Pure common code, but every downstream inventory is built on it.
     */
    private static void storageHandlerInsertExtract(GameTestHelper helper) {
        // Slot 1 is deliberately narrow and picky, so limit and validity are exercised separately.
        IItemStorageHandler handler = new ItemStackStorageHandler(2) {
            @Override
            public int getSlotLimit(int slot) {
                return slot == 1 ? 4 : super.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot != 1 || stack.is(Items.STICK);
            }
        };

        helper.assertTrue(handler.isEmpty(), "a fresh handler was not empty");

        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.STICK, 16), false).isEmpty(),
                "inserting 16 sticks into an empty 64 slot left a remainder");
        helper.assertValueEqual(handler.getStackInSlot(0).getCount(), 16, "slot 0 count after insert");

        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.STICK, 8), true).isEmpty(),
                "a simulated insert of 8 more sticks reported a remainder");
        helper.assertValueEqual(handler.getStackInSlot(0).getCount(), 16, "slot 0 count after a SIMULATED insert");

        helper.assertFalse(handler.isItemValid(1, new ItemStack(Items.STONE)), "the picky slot accepted stone from isItemValid");
        helper.assertTrue(handler.isItemValid(1, new ItemStack(Items.STICK)), "the picky slot rejected sticks from isItemValid");
        helper.assertTrue(handler.insertItem(1, new ItemStack(Items.STONE), false).getCount() == 1,
                "inserting stone into the picky slot did not hand the whole stack back");
        helper.assertTrue(handler.getStackInSlot(1).isEmpty(), "the picky slot took stone anyway");

        ItemStack overLimit = handler.insertItem(1, new ItemStack(Items.STICK, 10), false);
        helper.assertValueEqual(overLimit.getCount(), 6, "remainder after inserting 10 sticks into a limit-4 slot");
        helper.assertValueEqual(handler.getStackInSlot(1).getCount(), 4, "slot 1 count against a limit of 4");

        helper.assertValueEqual(handler.extractItem(0, 8, true).getCount(), 8, "simulated extraction size");
        helper.assertValueEqual(handler.getStackInSlot(0).getCount(), 16, "slot 0 count after a SIMULATED extract");
        helper.assertValueEqual(handler.extractItem(0, 8, false).getCount(), 8, "real extraction size");
        helper.assertValueEqual(handler.getStackInSlot(0).getCount(), 8, "slot 0 count after a real extract");

        helper.succeed();
    }

    /**
     * A block entity's inventory reached the way a hopper reaches it, per face through the level:
     * what goes in through the abstraction is visible to vanilla and vice versa. This sided bridge
     * over an unknown block entity is the part most likely to diverge between the loaders.
     */
    private static void blockEntityHandlerFromLevel(GameTestHelper helper) {
        helper.setBlock(WORK, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(WORK, ChestBlockEntity.class);

        Optional<IItemStorageHandler> found = Services.INVENTORY.getItemStorageHandler(chest, Direction.UP);
        helper.assertTrue(found.isPresent(), "no item storage handler for a vanilla chest through the level");

        IItemStorageHandler handler = found.get();
        helper.assertValueEqual(handler.getSlots(), chest.getContainerSize(), "slot count seen through the abstraction");

        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.STICK, 8), false).isEmpty(),
                "inserting 8 sticks into an empty chest slot left a remainder");
        helper.assertTrue(chest.getItem(0).is(Items.STICK), "the chest itself does not hold the inserted sticks");
        helper.assertValueEqual(chest.getItem(0).getCount(), 8, "chest slot 0 count after insert");

        chest.setItem(1, new ItemStack(Items.STONE, 5));
        helper.assertTrue(handler.getStackInSlot(1).is(Items.STONE), "the abstraction cannot see what vanilla put in the chest");
        helper.assertValueEqual(handler.getStackInSlot(1).getCount(), 5, "count seen through the abstraction");

        helper.assertValueEqual(handler.extractItem(1, 5, true).getCount(), 5, "simulated extraction size");
        helper.assertValueEqual(chest.getItem(1).getCount(), 5, "chest slot 1 count after a SIMULATED extract");
        helper.assertValueEqual(handler.extractItem(1, 5, false).getCount(), 5, "real extraction size");
        helper.assertTrue(chest.getItem(1).isEmpty(), "the chest still holds stone after a real extract");

        helper.succeed();
    }

    /**
     * Sorting mods such as ClientSort group a menu's slots by {@code slot.container}, so every slot of
     * one handler has to share a container that covers all of it, and another handler gets its own.
     * A lock slot in the same handler gets its own too, so sorting never moves the lock.
     */
    private static void storageSlotsShareOneContainer(GameTestHelper helper) {
        IItemStorageHandler handler = new ItemStackStorageHandler(3) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !stack.is(Items.STONE);
            }
        };
        Slot first = new SlotStorageHandler(handler, 0, 0, 0);
        Slot last = new SlotStorageHandler(handler, 2, 0, 0);
        Slot other = new SlotStorageHandler(new ItemStackStorageHandler(3), 0, 0, 0);
        Slot lock = new LockSlot(handler, 0, 0, 0);

        helper.assertTrue(first.container == last.container, "two slots of one handler have different containers");
        helper.assertFalse(first.container == other.container, "slots of two handlers share a container");
        helper.assertFalse(lock.container == first.container, "a lock slot shares the container of the items it locks");
        helper.assertValueEqual(first.container.getContainerSize(), 3, "shared container size");

        handler.insertItem(2, new ItemStack(Items.STICK, 5), false);
        helper.assertValueEqual(first.container.getItem(2).getCount(), 5, "container count of a stack put in the handler");
        helper.assertFalse(first.container.canPlaceItem(0, new ItemStack(Items.STONE)), "the container allowed an item the handler refuses");
        // ClientSort moves an empty slot's stack like any other and gives up if the slot refuses it.
        helper.assertTrue(first.mayPlace(ItemStack.EMPTY), "a storage slot refused to be emptied");
        helper.assertTrue(lock.mayPlace(ItemStack.EMPTY), "a lock slot refused to be emptied");
        helper.assertFalse(lock.mayPlace(new ItemStack(Items.STICK)), "a lock slot took a stick");

        helper.succeed();
    }
}
