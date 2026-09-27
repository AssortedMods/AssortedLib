package com.grim3212.assorted.lib.core.storage;

/**
 * A menu that holds a storage block open. One {@link BaseStorageBlockEntity} cannot recognise when it
 * recounts reads as nobody, driving the count negative on close and freezing the lid.
 */
public interface IStorageMenu {

    /** Whether this menu is holding {@code blockEntity} open. */
    boolean holdsOpen(BaseStorageBlockEntity blockEntity);
}
