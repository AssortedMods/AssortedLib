package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.client.key.ForgeKeyConflictContextPlatformDelegate;
import com.grim3212.assorted.lib.client.key.IKeyConflictHelper;
import com.grim3212.assorted.lib.client.key.KeyModifier;
import com.grim3212.assorted.lib.client.key.PlatformKeyConflictContextForgeDelegate;
import com.grim3212.assorted.lib.platform.services.IKeyBindingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public class ForgeKeyBindingHelper implements IKeyBindingHelper {

    @Override
    public IKeyConflictHelper getGuiKeyConflictContext() {
        return new ForgeKeyConflictContextPlatformDelegate(KeyConflictContext.GUI);
    }

    @Override
    public IKeyConflictHelper getInGameKeyConflictContext() {
        return new ForgeKeyConflictContextPlatformDelegate(KeyConflictContext.IN_GAME);
    }

    @Override
    public KeyMapping createNew(
            final String translationKey, final IKeyConflictHelper keyConflictContext, final InputConstants.Type inputType, final int key, final ResourceLocation category) {
        return new KeyMapping(
                translationKey,
                new PlatformKeyConflictContextForgeDelegate(keyConflictContext),
                inputType,
                key,
                getCategory(category)
        );
    }

    @Override
    public KeyMapping createNew(
            final String translationKey,
            final IKeyConflictHelper keyConflictContext,
            final KeyModifier keyModifier,
            final InputConstants.Type inputType,
            final int key,
            final ResourceLocation category) {
        return new KeyMapping(
                translationKey,
                new PlatformKeyConflictContextForgeDelegate(keyConflictContext),
                makePlatformSpecific(keyModifier),
                inputType,
                key,
                getCategory(category)
        );
    }

    @Override
    public boolean isKeyConflictOfActive(final KeyMapping keybinding) {
        return keybinding.getKeyConflictContext().isActive();
    }

    @Override
    public boolean isKeyModifierActive(final KeyMapping keybinding) {
        return keybinding.getKeyModifier().isActive(keybinding.getKeyConflictContext());
    }

    /**
     * A key mapping's category is its label's translation key, {@code key.category.<namespace>.<path>};
     * the controls screen groups mappings by that string.
     */
    public static String getCategory(final ResourceLocation category) {
        return "key.category." + category.getNamespace() + "." + category.getPath();
    }

    private static net.neoforged.neoforge.client.settings.KeyModifier makePlatformSpecific(final KeyModifier keyModifier) {
        return switch (keyModifier) {
            case CONTROL -> net.neoforged.neoforge.client.settings.KeyModifier.CONTROL;
            case SHIFT -> net.neoforged.neoforge.client.settings.KeyModifier.SHIFT;
            case ALT -> net.neoforged.neoforge.client.settings.KeyModifier.ALT;
        };
    }
}
