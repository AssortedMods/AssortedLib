package com.grim3212.assorted.lib.core.storage.ender;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * The locked ender chest inventories, one per lock code, saved through a {@link SavedDataType}
 * codec.
 */
public class EnderSavedData extends SavedData implements IEnderData {

    private static final int INVENTORY_SIZE = 27;

    public static final Codec<EnderSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, ItemStack.OPTIONAL_CODEC.listOf()).optionalFieldOf("locked_ender_chests", Map.of()).forGetter(EnderSavedData::writeChests)
    ).apply(instance, EnderSavedData::new));

    // Assorted Storage's id, kept so worlds saved before this moved here find their inventories
    public static final SavedDataType<EnderSavedData> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("assortedstorage", "locked_ender_saved_data"), EnderSavedData::new, CODEC, DataFixTypes.LEVEL);

    private final EnderData enderData = new EnderData();

    public EnderSavedData() {
    }

    private EnderSavedData(Map<String, List<ItemStack>> chests) {
        chests.forEach((code, items) -> {
            LockedEnderChestInventory inventory = this.enderData.getInventory(code);
            for (int slot = 0; slot < Math.min(items.size(), inventory.getSlots()); slot++) {
                inventory.getStacks().set(slot, items.get(slot));
            }
        });
    }

    private static final EnderSavedData DUMMY_SAVE = new EnderSavedData() {
        private final LockedEnderChestInventory inv = new LockedEnderChestInventory(this, "", INVENTORY_SIZE) {
            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return false;
            }
        };

        @Override
        public LockedEnderChestInventory getInventory(String code) {
            return inv;
        }
    };

    public static EnderSavedData get(Level world) {
        if (!(world instanceof ServerLevel)) {
            return DUMMY_SAVE;
        }

        SavedDataStorage storage = world.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }

    @Override
    public void markDirty() {
        setDirty();
    }

    @Override
    public LockedEnderChestInventory getInventory(String code) {
        return enderData.getInventory(code);
    }

    private Map<String, List<ItemStack>> writeChests() {
        Map<String, List<ItemStack>> chests = Maps.newHashMap();
        this.enderData.enderChests.forEach((code, inventory) -> chests.put(code, List.copyOf(inventory.getStacks())));
        return chests;
    }

    private class EnderData implements IEnderData {
        private final Map<String, LockedEnderChestInventory> enderChests = Maps.newHashMap();

        @Override
        public LockedEnderChestInventory getInventory(String code) {
            LockedEnderChestInventory inventory = enderChests.get(code);

            if (inventory == null) {
                inventory = new LockedEnderChestInventory(this, code, INVENTORY_SIZE);
                enderChests.put(code, inventory);
            }

            return inventory;
        }

        @Override
        public void markDirty() {
            setDirty();
        }
    }
}
