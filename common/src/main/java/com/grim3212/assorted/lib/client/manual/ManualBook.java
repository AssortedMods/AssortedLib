package com.grim3212.assorted.lib.client.manual;

import com.grim3212.assorted.lib.LibConstants;
import net.minecraft.resources.ResourceLocation;

/** The book's sprites. Positions and colours are data instead; see {@link ManualBookStyle}. */
public final class ManualBook {

    private ManualBook() {
    }

    public static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "manual/" + name);
    }

    public static final class Sprites {
        public static final ResourceLocation PAGE_FORWARD = sprite("page_forward");
        public static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED = sprite("page_forward_highlighted");
        public static final ResourceLocation PAGE_BACKWARD = sprite("page_backward");
        public static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED = sprite("page_backward_highlighted");
        public static final ResourceLocation HOME = sprite("home");
        public static final ResourceLocation HOME_HIGHLIGHTED = sprite("home_highlighted");

        /** Gold behind a slot that takes any of several stacks. */
        public static final ResourceLocation TAG_SLOT = sprite("tag_slot");

        /** Marks a shapeless recipe. */
        public static final ResourceLocation SHAPELESS = sprite("shapeless");

        /** Beside the crosshair, when what it is on has a page. */
        public static final ResourceLocation PAGE_AVAILABLE = sprite("page_available");

        private Sprites() {
        }
    }
}
