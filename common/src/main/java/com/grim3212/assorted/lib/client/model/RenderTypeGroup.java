package com.grim3212.assorted.lib.client.model;

import net.minecraft.client.renderer.RenderType;

/**
 * Equivalent render types: one on {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#BLOCK}, two
 * on {@code NEW_ENTITY}. {@code entityFabulous} may use fabulous render targets, or equal
 * {@code entity}.
 */
public record RenderTypeGroup(RenderType block, RenderType entity, RenderType entityFabulous) {
    public static final RenderTypeGroup EMPTY = new RenderTypeGroup(null, null, null);

    public RenderTypeGroup {
        if ((block == null) != (entity == null) || (block == null) != (entityFabulous == null))
            throw new IllegalArgumentException("The render types in a group must either be all null, or all non-null.");
    }

    public RenderTypeGroup(RenderType block, RenderType entity) {
        this(block, entity, entity);
    }

    /**
     * {@return true if this group has render types or not. It either has all, or none}
     */
    public boolean isEmpty() {
        // We throw an exception in the constructor if nullability doesn't match, so checking this is enough
        return block == null;
    }
}
