package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.annotations.LoaderImplement;
import com.grim3212.assorted.lib.mixin.data.AccessorBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class LibBlockLootProvider extends VanillaBlockLoot {

    private final Supplier<Iterable<Block>> knownBlocks;

    public LibBlockLootProvider(HolderLookup.Provider registries, Supplier<Iterable<Block>> knownBlocks) {
        super(registries);
        this.knownBlocks = knownBlocks;
    }

    @Override
    public abstract void generate();

    /** Vanilla's loop over only this mod's blocks; NeoForge's patch can narrow it, the vanilla jar cannot. */
    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        this.generate();
        Set<ResourceKey<LootTable>> set = new HashSet<>();

        for (Block block : getKnownBlocks()) {
            if (block.isEnabled(((AccessorBlockLootSubProvider) this).assortedlib_getEnabledFeatures())) {
                ResourceKey<LootTable> lootTable = block.getLootTable();
                // A block without drops points at the empty table, which is never written.
                if (lootTable != BuiltInLootTables.EMPTY && set.add(lootTable)) {
                    LootTable.Builder builder = this.map.remove(lootTable);
                    if (builder == null) {
                        throw new IllegalStateException(String.format(Locale.ROOT, "Missing loottable '%s' for '%s'", lootTable.location(), BuiltInRegistries.BLOCK.getKey(block)));
                    }

                    output.accept(lootTable, builder);
                }
            }
        }

        if (!this.map.isEmpty()) {
            throw new IllegalStateException("Created block loot tables for non-blocks: " + this.map.keySet());
        }
    }

    @LoaderImplement(loader = LoaderImplement.Loader.FORGE, value = "BlockLootSubProvider")
    public Iterable<Block> getKnownBlocks() {
        return this.knownBlocks.get();
    }

}
