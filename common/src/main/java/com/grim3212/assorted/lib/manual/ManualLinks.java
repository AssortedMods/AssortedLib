package com.grim3212.assorted.lib.manual;

import com.grim3212.assorted.lib.manual.IManualEntry.IManualBlock;
import com.grim3212.assorted.lib.manual.IManualEntry.IManualEntity;
import com.grim3212.assorted.lib.manual.IManualEntry.IManualItem;
import com.grim3212.assorted.lib.registry.IRegistryObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What right clicking something with the manual opens. Read from
 * {@code assets/<modId>/manual/links.json}, or registered in code; an {@link IManualEntry} is asked
 * before either, and the file wins over code so a resource pack can re-point a link.
 * <p>
 * Keyed by id, not by the block or item: a {@code DeferredRegister} entry is not bound yet while
 * mods are being constructed, which is when code links are registered.
 */
public final class ManualLinks {

    // Concurrent: NeoForge constructs mods in parallel and each registers its own.
    private static final Map<ResourceLocation, ManualPageRef> BLOCKS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ManualPageRef> ITEMS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ManualPageRef> ENTITIES = new ConcurrentHashMap<>();

    private static volatile Loaded loaded = Loaded.EMPTY;

    private ManualLinks() {
    }

    public static void linkBlock(ResourceLocation blockId, ManualPageRef page) {
        BLOCKS.put(blockId, page);
    }

    public static void linkItem(ResourceLocation itemId, ManualPageRef page) {
        ITEMS.put(itemId, page);
    }

    public static void linkEntity(ResourceLocation entityId, ManualPageRef page) {
        ENTITIES.put(entityId, page);
    }

    public static void linkBlock(IRegistryObject<? extends Block> block, ManualPageRef page) {
        linkBlock(block.getId(), page);
    }

    public static void linkItem(IRegistryObject<? extends Item> item, ManualPageRef page) {
        linkItem(item.getId(), page);
    }

    public static void linkEntity(IRegistryObject<? extends EntityType<?>> entityType, ManualPageRef page) {
        linkEntity(entityType.getId(), page);
    }

    /** Block and its item share a page. Matched by id, which a normally registered block item shares. */
    public static void linkWithItem(IRegistryObject<? extends Block> block, ManualPageRef page) {
        linkBlock(block.getId(), page);
        linkItem(block.getId(), page);
    }

    /** For an already bound object, such as a vanilla block. */
    public static void link(Block block, ManualPageRef page) {
        linkBlock(BuiltInRegistries.BLOCK.getKey(block), page);
    }

    public static void link(Item item, ManualPageRef page) {
        linkItem(BuiltInRegistries.ITEM.getKey(item), page);
    }

    public static void link(EntityType<?> entityType, ManualPageRef page) {
        linkEntity(BuiltInRegistries.ENTITY_TYPE.getKey(entityType), page);
    }

    /** Replaces every link read from resources. Called by the manual's loader on each reload. */
    public static void setLoaded(Loaded links) {
        loaded = links;
    }

    @Nullable
    public static ManualPageRef pageFor(@Nullable Level level, @Nullable BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof IManualBlock manualBlock) {
            ManualPageRef page = manualBlock.getManualPage(level, pos, state);
            if (page != null) {
                return page;
            }
        }

        return lookUp(BuiltInRegistries.BLOCK.getKey(state.getBlock()), loaded.blocks(), BLOCKS);
    }

    @Nullable
    public static ManualPageRef pageFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        if (stack.getItem() instanceof IManualItem manualItem) {
            ManualPageRef page = manualItem.getManualPage(stack);
            if (page != null) {
                return page;
            }
        }

        return lookUp(BuiltInRegistries.ITEM.getKey(stack.getItem()), loaded.items(), ITEMS);
    }

    /**
     * An {@link ItemFrame} resolves to the page of the item on display, unless the frame itself was
     * linked.
     */
    @Nullable
    public static ManualPageRef pageFor(Entity entity) {
        if (entity instanceof IManualEntity manualEntity) {
            ManualPageRef page = manualEntity.getManualPage(entity);
            if (page != null) {
                return page;
            }
        }

        ManualPageRef linked = lookUp(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), loaded.entities(), ENTITIES);
        if (linked != null) {
            return linked;
        }

        return entity instanceof ItemFrame frame ? pageFor(frame.getItem()) : null;
    }

    private static @Nullable ManualPageRef lookUp(ResourceLocation id, Map<ResourceLocation, ManualPageRef> fromData,
                                                  Map<ResourceLocation, ManualPageRef> fromCode) {
        ManualPageRef page = fromData.get(id);
        return page != null ? page : fromCode.get(id);
    }

    /** One reload's worth of links, by kind. */
    public record Loaded(Map<ResourceLocation, ManualPageRef> blocks, Map<ResourceLocation, ManualPageRef> items,
                         Map<ResourceLocation, ManualPageRef> entities) {

        public static final Loaded EMPTY = new Loaded(Map.of(), Map.of(), Map.of());

        /** Flattens the groups a {@code links.json} is written as into a lookup per kind. */
        public static Loaded of(List<Group> groups) {
            Map<ResourceLocation, ManualPageRef> blocks = new HashMap<>();
            Map<ResourceLocation, ManualPageRef> items = new HashMap<>();
            Map<ResourceLocation, ManualPageRef> entities = new HashMap<>();

            for (Group group : groups) {
                // A block's item shares its id, so listing a block covers the item that places it.
                group.blocks().forEach(id -> {
                    blocks.put(id, group.page());
                    items.put(id, group.page());
                });
                group.items().forEach(id -> items.put(id, group.page()));
                group.entities().forEach(id -> entities.put(id, group.page()));
            }

            return new Loaded(Map.copyOf(blocks), Map.copyOf(items), Map.copyOf(entities));
        }
    }

    /**
     * Everything that opens one page, grouped so a page is named once rather than per id.
     * {@code blocks} also links the item that places them.
     */
    public record Group(ManualPageRef page, List<ResourceLocation> blocks, List<ResourceLocation> items, List<ResourceLocation> entities) {

        public static final Codec<Group> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ManualPageRef.CODEC.fieldOf("page").forGetter(Group::page),
                ResourceLocation.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(Group::blocks),
                ResourceLocation.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Group::items),
                ResourceLocation.CODEC.listOf().optionalFieldOf("entities", List.of()).forGetter(Group::entities)
        ).apply(instance, Group::new));

        /** The shape of a {@code links.json}: a {@code links} array of these. */
        public static final Codec<List<Group>> FILE_CODEC = CODEC.listOf().fieldOf("links").codec();
    }
}
