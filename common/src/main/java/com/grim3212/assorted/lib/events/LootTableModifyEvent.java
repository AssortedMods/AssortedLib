package com.grim3212.assorted.lib.events;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

public class LootTableModifyEvent extends GenericEvent {
    private final LootTable lootTable;
    private final Identifier id;
    private final LootTableModificationContext context;
    private final boolean builtin;

    public LootTableModifyEvent(LootTable lootTable, Identifier id, LootTableModificationContext context, boolean builtin) {
        this.lootTable = lootTable;
        this.id = id;
        this.context = context;
        this.builtin = builtin;
    }

    public LootTable getLootTable() {
        return lootTable;
    }

    public Identifier getId() {
        return id;
    }

    public LootTableModificationContext getContext() {
        return context;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public interface LootTableModificationContext {
        /**
         * Adds a pool to the loot table.
         *
         * @param pool the pool to add
         */
        void addPool(LootPool pool);

        /**
         * Adds a pool to the loot table.
         *
         * @param pool the pool to add
         */
        default void addPool(LootPool.Builder pool) {
            addPool(pool.build());
        }

        /** Another loot table, for {@code NestedLootTable.lootTableReference}, which takes the holder now. */
        Holder<LootTable> lootTable(ResourceKey<LootTable> key);
    }
}
