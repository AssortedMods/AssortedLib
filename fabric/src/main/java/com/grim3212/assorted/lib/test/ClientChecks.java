package com.grim3212.assorted.lib.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.util.ArrayList;
import java.util.List;

/** The client checks every Assorted mod runs from its Fabric client gametest, inside a world, as recipes and tags arrive from one. */
public final class ClientChecks {

    private ClientChecks() {
    }

    /** Fails when anything in the namespace draws the missing model or texture, or the manual has a page that would draw nothing. */
    public static void assertDrawsRight(ClientGameTestContext context, String modId) {
        context.waitFor(client -> ManualCheck.recipesHaveSynced(), 200);
        List<String> problems = context.computeOnClient(client -> {
            List<String> found = new ArrayList<>(ClientModelCheck.problems(modId));
            found.addAll(ManualCheck.problems());
            return found;
        });
        if (!problems.isEmpty()) {
            throw new AssertionError(modId + " does not draw right:\n  " + String.join("\n  ", problems));
        }
    }
}
