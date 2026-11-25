package com.grim3212.assorted.lib.manual;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.creative.CreativeTabItems;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.registry.IRegistryObject;
import com.grim3212.assorted.lib.registry.RegistryProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

/** The library's only item; it lives here because every Assorted mod already depends on the library. */
public class LibItems {

    public static final RegistryProvider<Item> ITEMS = RegistryProvider.create(Registries.ITEM, LibConstants.MOD_ID);

    public static final IRegistryObject<Item> INSTRUCTION_MANUAL = register("instruction_manual", InstructionManualItem::new);

    /** Vanilla's tab rather than one of the library's own, for a single item. */
    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("tools_and_utilities"));

    private static <T extends Item> IRegistryObject<T> register(final String name, final Function<Item.Properties, ? extends T> factory) {
        return ITEMS.register(name, () -> factory.apply(new Item.Properties()));
    }

    public static void init() {
        Services.PLATFORM.modifyCreativeTab(TOOLS_AND_UTILITIES, LibItems::creativeItems);
    }

    private static List<ItemStack> creativeItems() {
        CreativeTabItems items = new CreativeTabItems();
        items.add(INSTRUCTION_MANUAL.get());
        return items.getItems();
    }
}
