package com.grim3212.assorted.lib.util;

import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DyeHelper {
    public static TagKey<Item> getDyeTag(DyeColor color) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(LibCommonTags.COMMON_NAMESPACE, "dyes/" + color.getName()));
    }

    public static DyeColor getColor(ItemStack stack) {
        if (stack.getItem() instanceof DyeItem dyeItem) {
            return dyeItem.getDyeColor();
        }

        for (int x = 0; x < DyeColor.BLACK.getId(); x++) {
            DyeColor color = DyeColor.byId(x);
            if (stack.is(getDyeTag(color)))
                return color;
        }

        return null;
    }

    public static String[] getDyeNames() {
        DyeColor[] states = DyeColor.values();
        String[] names = new String[states.length];

        for (int i = 0; i < states.length; i++) {
            names[i] = states[i].name();
        }
        return names;
    }

    /**
     * Collects vanilla's dyed block family named {@code <color>_<suffix>}, for every {@link DyeColor}.
     * 1.21.1 has no collection object for these families, so the family is generated from the dye
     * list and the vanilla id pattern rather than spelled out.
     */
    private static Map<DyeColor, Block> byDye(String suffix) {
        return Util.make(Maps.newEnumMap(DyeColor.class), (map) -> {
            for (DyeColor color : DyeColor.values()) {
                map.put(color, BuiltInRegistries.BLOCK.getOrThrow(ResourceKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace(color.getName() + "_" + suffix))));
            }
        });
    }

    public static final Map<DyeColor, Block> WOOL_BY_DYE = byDye("wool");

    public static final Map<DyeColor, Block> CONCRETE_BY_DYE = byDye("concrete");

    public static final Map<DyeColor, Block> CONCRETE_POWDER_BY_DYE = byDye("concrete_powder");

    public static final Map<DyeColor, Block> CARPET_BY_DYE = byDye("carpet");

    public static final List<Map<DyeColor, Block>> BLOCKS_BY_DYE = Arrays.asList(WOOL_BY_DYE, CONCRETE_BY_DYE, CONCRETE_POWDER_BY_DYE, CARPET_BY_DYE);
}
