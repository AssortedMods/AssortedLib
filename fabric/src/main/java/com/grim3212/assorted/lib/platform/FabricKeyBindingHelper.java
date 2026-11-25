package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.client.key.IKeyConflictHelper;
import com.grim3212.assorted.lib.client.key.KeyModifier;
import com.grim3212.assorted.lib.platform.services.IKeyBindingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;



public class FabricKeyBindingHelper implements IKeyBindingHelper {

    @Override
    public IKeyConflictHelper getGuiKeyConflictContext() {
        return FabricGuiKeyConflictHelper.INSTANCE;
    }

    @Override
    public IKeyConflictHelper getInGameKeyConflictContext() {
        return FabricInGameKeyConflictHelper.INSTANCE;
    }

    @Override
    public KeyMapping createNew(
            final String translationKey, final IKeyConflictHelper keyConflictContext, final InputConstants.Type inputType, final int key, final ResourceLocation category) {
        return new KeyMapping(
                translationKey,
                inputType,
                key,
                category(category)
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
        return new ModifiedKeyMapping(translationKey, inputType, key, category(category), keyConflictContext, keyModifier);
    }

    @Override
    public boolean isKeyConflictOfActive(final KeyMapping keybinding) {
        if (keybinding instanceof ModifiedKeyMapping modifiedKeyMapping) {
            return modifiedKeyMapping.context.isActive();
        }

        return true;
    }

    @Override
    public boolean isKeyModifierActive(final KeyMapping keybinding) {
        if (keybinding instanceof ModifiedKeyMapping modifiedKeyMapping) {
            return modifiedKeyMapping.isKeyModifierActive();
        }

        return true;
    }

    /**
     * A key mapping's category is its translation key, {@code key.category.<namespace>.<path>}. Fabric's
     * {@code KeyBindingHelper#registerKeyBinding} adds it to vanilla's sort order when the mapping is registered.
     */
    private static String category(final ResourceLocation category) {
        return category.toLanguageKey("key.category");
    }

    private static final class FabricGuiKeyConflictHelper implements IKeyConflictHelper {

        private static final FabricGuiKeyConflictHelper INSTANCE = new FabricGuiKeyConflictHelper();

        private FabricGuiKeyConflictHelper() {
        }

        @Override
        public boolean isActive() {
            return Minecraft.getInstance().screen != null;
        }

        @Override
        public boolean conflicts(IKeyConflictHelper other) {
            return this == other;
        }
    }

    private static final class FabricInGameKeyConflictHelper implements IKeyConflictHelper {

        private static final FabricInGameKeyConflictHelper INSTANCE = new FabricInGameKeyConflictHelper();

        private FabricInGameKeyConflictHelper() {
        }

        @Override
        public boolean isActive() {
            return !FabricGuiKeyConflictHelper.INSTANCE.isActive();
        }

        @Override
        public boolean conflicts(IKeyConflictHelper other) {
            return this == other;
        }
    }

    private static class ModifiedKeyMapping extends KeyMapping {
        private final IKeyConflictHelper context;
        private final KeyModifier keyModifier;

        public ModifiedKeyMapping(
                final String translationKey,
                final InputConstants.Type inputType,
                final int key,
                final String category,
                final IKeyConflictHelper context,
                final KeyModifier keyModifier) {
            super(translationKey,
                    inputType,
                    key,
                    category);
            this.context = context;
            this.keyModifier = keyModifier;
        }

        @Override
        public boolean isDown() {
            return super.isDown() && isKeyModifierActive();
        }

        private boolean isKeyModifierActive() {
            // Screen's helpers read the live keyboard, and treat Command as control on macOS.
            return switch (keyModifier) {
                case CONTROL -> Screen.hasControlDown();
                case SHIFT -> Screen.hasShiftDown();
                case ALT -> Screen.hasAltDown();
            };
        }

        @Override
        public Component getTranslatedKeyMessage() {
            return getKeyModifierMessage().append(super.getTranslatedKeyMessage());
        }

        private MutableComponent getKeyModifierMessage() {
            return switch (keyModifier) {
                case CONTROL -> Component.literal("CTRL + ");
                case SHIFT -> Component.literal("SHIFT + ");
                case ALT -> Component.literal("ALT + ");
            };
        }
    }
}
