package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.annotations.LoaderImplement;
import com.grim3212.assorted.lib.mixin.data.AccessorBlockLootSubProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public abstract class LibBlockLootProvider extends VanillaBlockLoot {

    private final Supplier<Iterable<Block>> knownBlocks;

    public LibBlockLootProvider(LootTableSubProvider.Context context, Supplier<Iterable<Block>> knownBlocks) {
        super(context);
        this.knownBlocks = knownBlocks;
    }

    @Override
    public abstract void generate();

    /** Vanilla's loop over only this mod's blocks; NeoForge's patch can narrow it, the vanilla jar cannot. */
    @Override
    public void run() {
        this.generate();
        Set<ResourceKey<LootTable>> set = new HashSet<>();
        AccessorBlockLootSubProvider provider = ((AccessorBlockLootSubProvider) this);

        for (Block block : getKnownBlocks()) {
            if (block.isEnabled(provider.assortedlib_getEnabledFeatures())) {
                // Blocks without a loot table are simply empty now instead of pointing at BuiltInLootTables.EMPTY
                block.getLootTable().ifPresent((lootTable) -> {
                    if (set.add(lootTable)) {
                        LootTable.Builder loottable$builder = provider.assortedlib_getMap().remove(lootTable);
                        if (loottable$builder == null) {
                            throw new IllegalStateException(String.format(Locale.ROOT, "Missing loottable '%s' for '%s'", lootTable.identifier(), BuiltInRegistries.BLOCK.getKey(block)));
                        }

                        this.output.accept(lootTable, loottable$builder);
                    }
                });
            }
        }

        if (!provider.assortedlib_getMap().isEmpty()) {
            throw new IllegalStateException("Created block loot tables for non-blocks: " + provider.assortedlib_getMap().keySet());
        }
    }

    @LoaderImplement(loader = LoaderImplement.Loader.FORGE, value = "BlockLootSubProvider")
    public Iterable<Block> getKnownBlocks() {
        return this.knownBlocks.get();
    }

}
