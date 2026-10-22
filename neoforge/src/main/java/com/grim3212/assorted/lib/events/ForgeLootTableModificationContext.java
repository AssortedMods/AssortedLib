package com.grim3212.assorted.lib.events;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

public class ForgeLootTableModificationContext implements LootTableModifyEvent.LootTableModificationContext {
    private final LootTable table;
    private final HolderGetter.Provider registries;

    public ForgeLootTableModificationContext(LootTable table, HolderGetter.Provider registries) {
        this.table = table;
        this.registries = registries;
    }

    @Override
    public void addPool(LootPool pool) {
        table.addPool(pool);
    }

    @Override
    public Holder<LootTable> lootTable(ResourceKey<LootTable> key) {
        return this.registries.lookupOrThrow(Registries.LOOT_TABLE).getOrThrow(key);
    }
}
