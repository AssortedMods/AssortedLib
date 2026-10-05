package com.grim3212.assorted.lib.family;

import com.grim3212.assorted.lib.config.ConfigurationType;
import com.grim3212.assorted.lib.config.IConfigurationBuilder;
import com.grim3212.assorted.lib.platform.Services;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Every family's {@code <familyId>-parts} config, made once all mods have joined so each file lists every installed part.
 * NeoForge makes them from Lib's first registry event, Fabric the first time a switch is read.
 */
public final class FamilySwitches {

    // Concurrent: NeoForge constructs mods in parallel.
    private static final Map<String, Set<String>> MEMBERS = new ConcurrentHashMap<>();
    private static volatile Map<String, Map<String, Supplier<Boolean>>> files;
    private static volatile boolean madeOnFirstRead;

    private FamilySwitches() {
    }

    static void add(String modId, String familyId) {
        synchronized (FamilySwitches.class) {
            if (files != null) {
                throw new IllegalStateException(modId + " joined " + familyId + " after its switches were read; join while the mod is constructed");
            }
            MEMBERS.computeIfAbsent(familyId, family -> new TreeSet<>()).add(modId);
        }
    }

    /** Whether {@code modId}'s line in its family's file is on. */
    static boolean isEnabled(String familyId, String modId) {
        Map<String, Map<String, Supplier<Boolean>>> made = files;
        if (made == null) {
            if (!madeOnFirstRead) {
                throw new IllegalStateException("The switches of " + familyId + " are not loaded until every mod has been constructed");
            }
            made = make();
        }
        return made.get(familyId).get(modId).get();
    }

    /** Fabric has no event between the last mod's initializer and the first read, so the files wait for that read. */
    public static void makeOnFirstRead() {
        madeOnFirstRead = true;
    }

    /** Called by NeoForge once every mod is constructed, before configs load. Later calls do nothing. */
    public static void makeAll() {
        make();
    }

    private static synchronized Map<String, Map<String, Supplier<Boolean>>> make() {
        if (files != null) {
            return files;
        }

        Map<String, Map<String, Supplier<Boolean>>> made = new TreeMap<>();
        MEMBERS.forEach((familyId, members) -> {
            IConfigurationBuilder builder = Services.CONFIG.createBuilder(ConfigurationType.NOT_SYNCED, familyId + "-parts");
            Map<String, Supplier<Boolean>> switches = new TreeMap<>();
            for (String member : members) {
                switches.put(member, builder.defineBoolean("parts." + member, true, "Set this to false to turn off " + member
                        + ". Its items leave the creative menu and its recipes, manual chapters, world generation and spawns stop. Anything already in a world stays."));
            }
            builder.setup();
            made.put(familyId, Map.copyOf(switches));
        });
        files = Map.copyOf(made);
        return files;
    }
}
