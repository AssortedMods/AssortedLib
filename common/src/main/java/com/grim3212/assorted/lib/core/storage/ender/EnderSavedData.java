package com.grim3212.assorted.lib.core.storage.ender;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.grim3212.assorted.lib.LibConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * The locked ender chest inventories, one per lock code, saved through {@link #CODEC} into the
 * overworld's data storage.
 */
public class EnderSavedData extends SavedData implements IEnderData {

    private static final int INVENTORY_SIZE = 27;

    public static final Codec<EnderSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, ItemStack.OPTIONAL_CODEC.listOf()).optionalFieldOf("locked_ender_chests", Map.of()).forGetter(EnderSavedData::writeChests)
    ).apply(instance, EnderSavedData::new));

    // Assorted Storage's file name, kept so worlds saved before this moved here find their inventories
    public static final String ID = "locked_ender_saved_data";
    public static final SavedData.Factory<EnderSavedData> FACTORY = new SavedData.Factory<>(EnderSavedData::new, EnderSavedData::load, DataFixTypes.LEVEL);

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

    // The client's stand-in, which only mirrors what the server sends. It accepts any item, as a vanilla
    // client container does, since sorting mods skip the empty slots of one that refuses them.
    private static final EnderSavedData DUMMY_SAVE = new EnderSavedData() {
        private final LockedEnderChestInventory inv = new LockedEnderChestInventory(this, "", INVENTORY_SIZE) {
            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return stack;
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

        DimensionDataStorage storage = world.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(FACTORY, ID);
    }

    private static EnderSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial(error -> LibConstants.LOG.error("Failed to read locked ender chests: {}", error))
                .orElseGet(EnderSavedData::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this)
                .resultOrPartial(error -> LibConstants.LOG.error("Failed to save locked ender chests: {}", error))
                .ifPresent(encoded -> tag.merge((CompoundTag) encoded));
        return tag;
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
