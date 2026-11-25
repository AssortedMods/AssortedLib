package com.grim3212.assorted.lib.client.render.entity;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class EntityRenderers {

    public interface EntityRendererConsumer {
        <E extends Entity> void accept(EntityType<? extends E> entityType, EntityRendererProvider<E> entityRendererFactory);
    }

    public interface BlockEntityRendererConsumer {
        <E extends BlockEntity> void accept(BlockEntityType<? extends E> entityType, BlockEntityRendererProvider<E> entityRendererFactory);
    }
}
