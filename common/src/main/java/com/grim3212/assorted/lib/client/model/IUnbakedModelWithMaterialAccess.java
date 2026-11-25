package com.grim3212.assorted.lib.client.model;

import net.minecraft.client.resources.model.Material;

public interface IUnbakedModelWithMaterialAccess {
    Material getMaterial(final String name);
}
