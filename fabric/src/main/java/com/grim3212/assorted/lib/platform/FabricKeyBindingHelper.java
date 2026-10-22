package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.client.key.IKeyConflictHelper;
import com.grim3212.assorted.lib.client.key.KeyModifier;
import com.grim3212.assorted.lib.platform.services.IKeyBindingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class FabricKeyBindingHelper implements IKeyBindingHelper {

    // KeyMapping.Category is a record around an Identifier that has to be registered once
    // (registering the same id twice throws) and derives its label itself, as
    // Component.translatable(id.toLanguageKey("key.category")). Cached so a repeated id reuses the
    // category rather than throwing.
    private static final Map<Identifier, KeyMapping.Category> CATEGORIES = new ConcurrentHashMap<>();

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
            final String translationKey, final IKeyConflictHelper keyConflictContext, final InputConstants.Type inputType, final int key, final Identifier category) {
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
            final Identifier category) {
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

    private static KeyMapping.Category category(final Identifier category) {
        return CATEGORIES.computeIfAbsent(category, KeyMapping.Category::register);
    }

    private static final class FabricGuiKeyConflictHelper implements IKeyConflictHelper {

        private static final FabricGuiKeyConflictHelper INSTANCE = new FabricGuiKeyConflictHelper();

        private FabricGuiKeyConflictHelper() {
        }

        @Override
        public boolean isActive() {
            // The current screen moved off Minecraft and onto its Gui in 26.x.
            return Minecraft.getInstance().gui.screen() != null;
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
                final KeyMapping.Category category,
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
            // Screen's static modifier helpers are gone; the keyboard state is read directly.
            return switch (keyModifier) {
                case CONTROL -> isModifierDown(InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL);
                case SHIFT -> isModifierDown(InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT);
                case ALT -> isModifierDown(InputConstants.KEY_LALT, InputConstants.KEY_RALT);
            };
        }

        private static boolean isModifierDown(final int left, final int right) {
            return InputConstants.isKeyDown(left) || InputConstants.isKeyDown(right);
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
