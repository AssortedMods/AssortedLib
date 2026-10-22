package com.grim3212.assorted.lib.events;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

public class FabricLootTableModificationContext implements LootTableModifyEvent.LootTableModificationContext {
    private final LootTable.Builder tableBuilder;
    private final HolderLookup.Provider registries;

    public FabricLootTableModificationContext(LootTable.Builder tableBuilder, HolderLookup.Provider registries) {
        this.tableBuilder = tableBuilder;
        this.registries = registries;
    }

    @Override
    public void addPool(LootPool pool) {
        tableBuilder.pool(pool);
    }

    @Override
    public void addPool(LootPool.Builder pool) {
        tableBuilder.withPool(pool);
    }

    @Override
    public Holder<LootTable> lootTable(ResourceKey<LootTable> key) {
        return this.registries.lookupOrThrow(Registries.LOOT_TABLE).getOrThrow(key);
    }
}
