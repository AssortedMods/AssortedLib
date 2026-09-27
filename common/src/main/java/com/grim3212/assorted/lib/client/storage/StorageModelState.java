package com.grim3212.assorted.lib.client.storage;

/**
 * Per-submission state for {@link ChestModel}: the lid angle in degrees, and whether the handle
 * ({@code true}) or the padlock ({@code false}) is drawn.
 */
public record StorageModelState(float doorAngle, boolean renderHandle) {

    public static final StorageModelState CLOSED_UNLOCKED = new StorageModelState(0.0F, true);

    public static final StorageModelState CLOSED_LOCKED = new StorageModelState(0.0F, false);
}
