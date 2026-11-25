package com.grim3212.assorted.lib.client.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.LibConstants;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * Development tooling for the documentation site: renders every item's inventory icon to a PNG
 * exactly the way the GUI draws it (same item model resolution, lighting, transforms and special
 * renderers as {@code GuiGraphics#renderItem}), then quits the game.
 * <p>
 * Inert unless the client is started with {@code -Dassortedlib.exportIcons=<directory>}. Nothing
 * is written anywhere near the resources, so no image ever ends up in a jar; the mods point it at
 * their {@code build/icons} from a dedicated run configuration. It lives in the library rather than
 * a dev-only source set because every other Assorted mod consumes the published jar, and this is
 * how each of them gets the exporter without carrying a copy.
 * <p>
 * It renders from inside a throwaway flat world ("Icon Export" in the run's saves, recreated on
 * every run) rather than the title screen: item components are only bound once a server has loaded its data packs, so
 * an {@link ItemStack} cannot even be created before then, and a level is what the inventory
 * renders with anyway.
 * <p>
 * Properties:
 * <ul>
 * <li>{@code assortedlib.exportIcons} - output directory (required to activate).</li>
 * <li>{@code assortedlib.exportIcons.size} - pixel size of an icon, default 96: one GUI pixel is
 * 6 image pixels, so the site's two and three times GUI scales are both exact downscales.</li>
 * <li>{@code assortedlib.exportIcons.namespaces} - comma separated item namespaces to export,
 * default every namespace loaded.</li>
 * <li>{@code assortedlib.exportIcons.stacks} - optional JSON file of extra stacks to render for
 * items whose icon depends on components (a coloured siding). Each entry is
 * {@code {"key": "ns:name--variant", "stack": {"id": "ns:item", "components": {...}}}}, the
 * stack in vanilla's recipe result format; the key becomes the file name.</li>
 * </ul>
 * Writes {@code <dir>/<namespace>/<path>.png} per icon and {@code <dir>/icons.json} listing every
 * icon with whether it is a flat (front-lit) item, which the site keeps pixel-crisp.
 */
@EventBusSubscriber(modid = LibConstants.MOD_ID, value = Dist.CLIENT)
public final class IconExporter {

    private static final String OUT_DIR = System.getProperty("assortedlib.exportIcons");
    private static final int SIZE = Integer.getInteger("assortedlib.exportIcons.size", 96);
    private static final String NAMESPACES = System.getProperty("assortedlib.exportIcons.namespaces");
    private static final String STACKS_FILE = System.getProperty("assortedlib.exportIcons.stacks");
    private static final String WORLD_ID = "Icon Export";
    private static final int FULL_BRIGHT = 15728880;
    /** Frames to let the world settle after the player exists, and the most to wait for it. */
    private static final int SETTLE_FRAMES = 20;
    private static final int WORLD_TIMEOUT_FRAMES = 20 * 60 * 5;

    private enum Phase { TITLE, WORLD, DONE }

    private static Phase phase = Phase.TITLE;
    private static int frames;

    private IconExporter() {
    }

    @SubscribeEvent
    public static void onFrame(final RenderFrameEvent.Post event) {
        if (OUT_DIR == null || phase == Phase.DONE) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        switch (phase) {
            case TITLE -> {
                // Set once the initial resource reload is done and the title screen is up.
                if (mc.isGameLoadFinished()) {
                    phase = Phase.WORLD;
                    // Queued rather than run here: this frame holds the window surface, so the
                    // frames doWorldLoad draws while it waits are skipped, the loading overlay
                    // never draws the frame that removes it, and the wait never ends.
                    mc.tell(() -> joinWorld(mc));
                }
            }
            case WORLD -> {
                if (mc.level != null && mc.player != null && ++frames >= SETTLE_FRAMES) {
                    phase = Phase.DONE;
                    try {
                        export(mc, Path.of(OUT_DIR));
                    } catch (Exception e) {
                        LibConstants.LOG.error("Icon export failed", e);
                    } finally {
                        mc.stop();
                    }
                } else if (mc.level == null && ++frames >= WORLD_TIMEOUT_FRAMES) {
                    phase = Phase.DONE;
                    LibConstants.LOG.error("Icon export gave up waiting for the world");
                    mc.stop();
                }
            }
            case DONE -> {
            }
        }
    }

    /**
     * Creates the export world as a peaceful creative flat world and joins it. A previous run's
     * copy is deleted first: reopening a world can stop at a confirmation screen, while a fresh
     * flat one loads in a few seconds with nothing to ask.
     */
    private static void joinWorld(final Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD_ID)) {
            try (LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().createAccess(WORLD_ID)) {
                access.deleteLevel();
            } catch (IOException e) {
                LibConstants.LOG.warn("Could not delete the previous {} world", WORLD_ID, e);
            }
        }
        final LevelSettings settings = new LevelSettings(WORLD_ID, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD_ID, settings, WorldOptions.defaultWithRandomSeed(),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }

    private record Job(String key, ItemStack stack) {
    }

    private static void export(final Minecraft mc, final Path outDir) throws IOException {
        final List<Job> jobs = new ArrayList<>();
        final Set<String> namespaces = NAMESPACES == null ? null : Arrays.stream(NAMESPACES.split(",")).map(String::trim).collect(Collectors.toSet());
        BuiltInRegistries.ITEM.entrySet().stream()
                .sorted((a, b) -> a.getKey().location().compareTo(b.getKey().location()))
                .forEach(entry -> {
                    final ResourceLocation id = entry.getKey().location();
                    if (namespaces == null || namespaces.contains(id.getNamespace())) {
                        jobs.add(new Job(id.toString(), new ItemStack(entry.getValue())));
                    }
                });
        jobs.addAll(extraStacks(mc));

        LibConstants.LOG.info("Exporting {} item icons at {}px to {}", jobs.size(), SIZE, outDir);
        Files.createDirectories(outDir);
        final TreeMap<String, JsonObject> manifest = new TreeMap<>();
        int failed = 0;
        try (Renderer renderer = new Renderer(mc, SIZE)) {
            for (Job job : jobs) {
                if (job.stack().isEmpty()) {
                    continue;
                }
                final String file = job.key().replaceFirst(":", "/") + ".png";
                try {
                    final boolean flat = renderer.render(job.stack(), outDir.resolve(file));
                    final JsonObject info = new JsonObject();
                    info.addProperty("file", file);
                    info.addProperty("flat", flat);
                    manifest.put(job.key(), info);
                } catch (Exception e) {
                    failed++;
                    LibConstants.LOG.warn("Could not render {}", job.key(), e);
                }
            }
        }

        final JsonObject root = new JsonObject();
        root.addProperty("size", SIZE);
        final JsonObject icons = new JsonObject();
        manifest.forEach(icons::add);
        root.add("icons", icons);
        Files.writeString(outDir.resolve("icons.json"), new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n");
        LibConstants.LOG.info("Exported {} icons ({} failed)", manifest.size(), failed);
    }

    /** The component-dependent stacks the site asks for, or nothing when there is no file. */
    private static List<Job> extraStacks(final Minecraft mc) throws IOException {
        final List<Job> jobs = new ArrayList<>();
        if (STACKS_FILE == null || !Files.isRegularFile(Path.of(STACKS_FILE))) {
            return jobs;
        }
        final JsonElement json = JsonParser.parseString(Files.readString(Path.of(STACKS_FILE)));
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, mc.level.registryAccess());
        for (JsonElement element : json.getAsJsonArray()) {
            final JsonObject entry = element.getAsJsonObject();
            final String key = entry.get("key").getAsString();
            ItemStack.CODEC.parse(ops, entry.get("stack"))
                    .resultOrPartial(error -> LibConstants.LOG.warn("Bad stack for {}: {}", key, error))
                    .ifPresent(stack -> jobs.add(new Job(key, stack)));
        }
        return jobs;
    }

    /**
     * One offscreen colour and depth target the size of an icon, drawn into the way
     * {@code GuiGraphics#renderItem} draws a slot, then read back and written as a PNG.
     */
    private static final class Renderer implements AutoCloseable {
        private final Minecraft mc;
        private final int size;
        private final TextureTarget target;

        Renderer(final Minecraft mc, final int size) {
            this.mc = mc;
            this.size = size;
            this.target = new TextureTarget(size, size, true, Minecraft.ON_OSX);
            this.target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }

        /** Draws the stack and saves it; returns whether the model is front-lit (a flat item). */
        boolean render(final ItemStack stack, final Path file) throws IOException {
            final BakedModel model = this.mc.getItemRenderer().getModel(stack, this.mc.level, this.mc.player, 0);
            final boolean flat = !model.usesBlockLight();

            this.target.clear(Minecraft.ON_OSX);
            this.target.bindWrite(true);

            // The GUI's own projection and view, at the icon's size.
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, this.size, this.size, 0.0F, 1000.0F, 21000.0F), VertexSorting.ORTHOGRAPHIC_Z);
            final Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            modelView.translation(0.0F, 0.0F, -11000.0F);
            RenderSystem.applyModelViewMatrix();

            final PoseStack pose = new PoseStack();
            pose.translate(this.size / 2.0F, this.size / 2.0F, 150.0F);
            pose.mulPose(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
            pose.scale(this.size, this.size, this.size);
            if (flat) {
                Lighting.setupForFlatItems();
            }
            final MultiBufferSource.BufferSource buffers = this.mc.renderBuffers().bufferSource();
            this.mc.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, pose, buffers, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            RenderSystem.disableDepthTest();
            buffers.endBatch();
            RenderSystem.enableDepthTest();
            if (flat) {
                Lighting.setupFor3DItems();
            }

            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();

            this.readBack(file);
            this.mc.getMainRenderTarget().bindWrite(true);
            return flat;
        }

        /** Copies the colour texture back from the GPU and writes it. */
        private void readBack(final Path file) throws IOException {
            try (NativeImage image = new NativeImage(this.size, this.size, false)) {
                RenderSystem.bindTexture(this.target.getColorTextureId());
                image.downloadTexture(0, false);
                image.flipY();
                for (int y = 0; y < this.size; y++) {
                    for (int x = 0; x < this.size; x++) {
                        // The target was blended with straight alpha onto transparent black, so its
                        // colour is premultiplied; PNG wants it straight. NativeImage packs ABGR.
                        image.setPixelRGBA(x, y, unpremultiply(image.getPixelRGBA(x, y)));
                    }
                }
                Files.createDirectories(file.getParent());
                image.writeToFile(file);
            }
        }

        private static int unpremultiply(final int abgr) {
            final int a = abgr >>> 24;
            if (a == 0 || a == 255) {
                return abgr;
            }
            final int r = Math.min(255, (abgr & 0xFF) * 255 / a);
            final int g = Math.min(255, ((abgr >>> 8) & 0xFF) * 255 / a);
            final int b = Math.min(255, ((abgr >>> 16) & 0xFF) * 255 / a);
            return (a << 24) | (b << 16) | (g << 8) | r;
        }

        @Override
        public void close() {
            this.target.destroyBuffers();
        }
    }

    static {
        if (OUT_DIR != null) {
            LibConstants.LOG.info("Icon export armed: {} ({}px)", OUT_DIR, SIZE);
        }
    }
}
