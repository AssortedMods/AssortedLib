package com.grim3212.assorted.lib.client.manual;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.client.manual.page.ImagePage;
import com.grim3212.assorted.lib.client.manual.page.ItemPage;
import com.grim3212.assorted.lib.client.manual.page.RecipePage;
import com.grim3212.assorted.lib.client.manual.page.TextPage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Page types a chapter's {@code type} field can name. Register from client init. */
public final class ManualPageTypes {

    // Concurrent: NeoForge constructs mods in parallel and each registers its own.
    private static final Map<ResourceLocation, MapCodec<? extends ManualPage>> BY_ID = new ConcurrentHashMap<>();
    private static final Map<MapCodec<? extends ManualPage>, ResourceLocation> BY_CODEC = new ConcurrentHashMap<>();

    /**
     * A map codec so a page's fields sit beside the {@code id} {@link ManualPageEntry} adds rather
     * than nesting. Dispatch resolves per use, since client init order across mods is not promised.
     */
    public static final MapCodec<ManualPage> MAP_CODEC = ResourceLocation.CODEC
            .dispatchMap("type", page -> BY_CODEC.get(page.codec()), ManualPageTypes::codecOf);

    public static final Codec<ManualPage> CODEC = MAP_CODEC.codec();

    private ManualPageTypes() {
    }

    /**
     * The page types the library itself provides. Called from client init, and from datagen, which
     * encodes chapters through these same codecs but never runs client init.
     */
    public static void bootstrap() {
        register(libId("text"), TextPage.CODEC);
        register(libId("image"), ImagePage.CODEC);
        register(libId("item"), ItemPage.CODEC);
        register(libId("recipe"), RecipePage.CODEC);
    }

    private static ResourceLocation libId(String name) {
        return ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, name);
    }

    public static void register(ResourceLocation id, MapCodec<? extends ManualPage> codec) {
        BY_ID.put(id, codec);
        BY_CODEC.put(codec, id);
    }

    private static MapCodec<? extends ManualPage> codecOf(ResourceLocation id) {
        MapCodec<? extends ManualPage> codec = BY_ID.get(id);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown manual page type: " + id);
        }
        return codec;
    }

    public static boolean isRegistered(ResourceLocation id) {
        return BY_ID.containsKey(id);
    }
}
