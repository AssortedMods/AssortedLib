package com.grim3212.assorted.lib.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

/** Reads json a mod writes by hand back through vanilla's codecs, so a format change fails datagen rather than the server. */
public final class HandWrittenData {

    /** Takes every entry and tag as present, as hand-written json names other mods' content that datagen never loads. */
    private static final RegistryOps<JsonElement> OPS = RegistryOps.create(JsonOps.INSTANCE, new RegistryOps.RegistryInfoLookup() {
        @Override
        public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
            @SuppressWarnings("unchecked") // The lookup is asked for a registry of T, whatever wildcard it is spelt with.
            AnyEntryLookup<T> lookup = new AnyEntryLookup<>((ResourceKey<? extends Registry<T>>) registryKey);
            return Optional.of(new RegistryOps.RegistryInfo<>(lookup.owner(), lookup, Lifecycle.stable()));
        }
    });

    private HandWrittenData() {
    }

    /** Returns the json unchanged once it reads as an advancement. */
    public static JsonObject advancement(JsonObject json) {
        Advancement.CODEC.parse(OPS, json).getOrThrow(error -> new IllegalStateException("Not an advancement this version reads: " + error + " in " + json));
        return json;
    }
}
