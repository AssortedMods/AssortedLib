package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.client.key.IKeyConflictHelper;
import com.grim3212.assorted.lib.client.key.KeyModifier;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public interface IKeyBindingHelper {

    /**
     * The conflict context which detects when a GUI is open.
     *
     * @return The conflict context.
     */
    IKeyConflictHelper getGuiKeyConflictContext();

    /**
     * The conflict context which detects when a GUI is not open.
     *
     * @return The conflict context.
     */
    IKeyConflictHelper getInGameKeyConflictContext();

    /**
     * Creates a key mapping, with a conflict context where the loader supports one.
     *
     * @param category the category id; its label {@code key.category.<namespace>.<path>} is the
     *                 caller's to translate
     */
    KeyMapping createNew(String translationKey, IKeyConflictHelper keyConflictContext, InputConstants.Type inputType, int key, ResourceLocation category);

    /**
     * Creates a key mapping, with a conflict context where the loader supports one.
     *
     * @param category the category id; its label {@code key.category.<namespace>.<path>} is the
     *                 caller's to translate
     */
    KeyMapping createNew(String translationKey, IKeyConflictHelper keyConflictContext, KeyModifier keyModifier, InputConstants.Type inputType, int key, ResourceLocation category);

    /** Whether the key mapping's conflict context is active. */
    boolean isKeyConflictOfActive(KeyMapping keybinding);

    /** Whether the key mapping's modifier is active. */
    boolean isKeyModifierActive(KeyMapping keybinding);
}
