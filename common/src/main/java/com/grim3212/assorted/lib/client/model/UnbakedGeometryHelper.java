package com.grim3212.assorted.lib.client.model;

import com.grim3212.assorted.lib.client.model.loaders.context.IModelBakingContext;
import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("resource")
public class UnbakedGeometryHelper {

    private UnbakedGeometryHelper() {
        throw new IllegalStateException("Can not instantiate an instance of: UnbakedGeometryHelper. This is a utility class");
    }

    // createUnbakedItemElements (the extruded "builtin/generated" item shape as elements) has no
    // replacement: ItemModelGenerator's element code is private and bakes straight to quads. A
    // model that wants that shape parents onto ItemModelGenerator.GENERATED_ITEM_MODEL_ID.

    /**
     * Creates {@linkplain CuboidModelElement cuboid elements} covering only the opaque pixels of
     * the sprite, so a flat item texture becomes a mask instead of a full quad. Only the mask: the
     * extruded edge elements are not obtainable (see the TODO above).
     */
    public static List<CuboidModelElement> createUnbakedItemMaskElements(int layerIndex, TextureAtlasSprite sprite) {
        var elements = new ArrayList<CuboidModelElement>();

        int width = sprite.contents().width(), height = sprite.contents().height();
        var bits = new BitSet(width * height);

        // For every frame in the texture, mark all the opaque pixels (this is what vanilla does too)
        sprite.contents().getUniqueFrames().forEach(frame -> {
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    if (!sprite.contents().isTransparent(frame, x, y))
                        bits.set(x + y * width);
        });

        // Scan in search of opaque pixels
        for (int y = 0; y < height; y++) {
            int xStart = -1;
            for (int x = 0; x < width; x++) {
                var opaque = bits.get(x + y * width);
                if (opaque == (xStart == -1)) // (opaque && -1) || (!opaque && !-1)
                {
                    if (xStart == -1) {
                        // We have found the start of a new segment, continue
                        xStart = x;
                        continue;
                    }

                    // The segment is over, expand down as far as possible
                    int yEnd = y + 1;
                    expand:
                    for (; yEnd < height; yEnd++)
                        for (int x2 = xStart; x2 <= x; x2++)
                            if (!bits.get(x2 + yEnd * width))
                                break expand;

                    // Mark all pixels in the area as visited
                    for (int i = xStart; i < x; i++)
                        for (int j = y; j < yEnd; j++)
                            bits.clear(i + j * width);

                    // Create element
                    int finalX = x, finalY = y, finalYEnd = yEnd, finalXStart = xStart;
                    elements.add(new CuboidModelElement(
                            new Vector3f(16 * finalXStart / (float) width, 16 - 16 * finalYEnd / (float) height, 7.5F),
                            new Vector3f(16 * finalX / (float) width, 16 - 16 * finalY / (float) height, 8.5F),
                            Util.make(new HashMap<>(), map -> {
                                for (Direction direction : Direction.values())
                                    map.put(direction, new CuboidFace(null, layerIndex, "layer" + layerIndex, null, Quadrant.R0));
                            })
                    ));

                    // Reset xStart
                    xStart = -1;
                }
            }
        }
        return elements;
    }

    /**
     * Turns a single {@link CuboidFace} into a {@link BakedQuad}.
     */
    public static BakedQuad bakeElementFace(ModelBaker baker, CuboidModelElement element, CuboidFace face, Material.Baked material, Direction direction, ModelState state) {
        return FaceBakery.bakeQuad(baker, element.from(), element.to(), face, material, direction, state, element.rotation(), element.shadeDirectionOverride(), element.lightEmission());
    }

    /**
     * Bakes a list of {@linkplain CuboidModelElement cuboid elements} and feeds the baked quads to a
     * {@linkplain IModelBuilder model builder}.
     */
    public static void bakeElements(ModelBaker baker, IModelBuilder<?> builder, List<CuboidModelElement> elements, TextureSlots textures, ModelState modelState, ModelDebugName name) {
        for (CuboidModelElement element : elements) {
            for (Map.Entry<Direction, CuboidFace> entry : element.faces().entrySet()) {
                var side = entry.getKey();
                var face = entry.getValue();
                var material = baker.materials().resolveSlot(textures, face.texture(), name);
                var quad = bakeElementFace(baker, element, face, material, side, modelState);
                if (face.cullForDirection() == null)
                    builder.addUnculledFace(quad);
                else
                    builder.addCulledFace(Direction.rotate(modelState.transformation().getMatrix(), face.cullForDirection()), quad);
            }
        }
    }

    /**
     * Bakes a list of {@linkplain CuboidModelElement cuboid elements} and returns the list of baked quads.
     */
    public static List<BakedQuad> bakeElements(ModelBaker baker, List<CuboidModelElement> elements, TextureSlots textures, ModelState modelState, ModelDebugName name) {
        if (elements.isEmpty())
            return List.of();
        return UnbakedCuboidGeometry.bake(elements, textures, baker, modelState, name).getAll();
    }

    /**
     * Extracts {@code namespace:path} from anything that looks like a resource pack texture file
     * path, so models exported by modelling software work unedited. Best effort: everything up to
     * {@code /assets/} is skipped, backslashes count as slashes, and
     * {@code .../assets/mymodid/textures/item/my_thing.png} gives {@code mymodid:item/my_thing}.
     */
    private static final Pattern FILESYSTEM_PATH_TO_RESLOC =
            Pattern.compile("(?:.*[\\\\/]assets[\\\\/](?<namespace>[a-z_-]+)[\\\\/]textures[\\\\/])?(?<path>[a-z_\\\\/-]+)\\.png");

    /**
     * Resolves a material that may name a filesystem path instead of a proper {@link Identifier}.
     * The atlas is not part of a {@link Material}; the baking {@code MaterialBaker} decides it.
     */
    public static Material resolveDirtyMaterial(@Nullable String tex, IModelBakingContext owner) {
        if (tex == null)
            return new Material(MissingTextureAtlasSprite.getLocation());
        if (tex.startsWith("#"))
            return owner.getMaterial(tex).orElse(null);

        // Attempt to convert a common (windows/linux/mac) filesystem path to a Identifier.
        // This makes no promises, if it doesn't work, too bad, fix your mtl file.
        Matcher match = FILESYSTEM_PATH_TO_RESLOC.matcher(tex);
        if (match.matches()) {
            String namespace = match.group("namespace");
            String path = match.group("path").replace("\\", "/");
            tex = namespace != null ? namespace + ":" + path : path;
        }

        return new Material(Identifier.parse(tex));
    }
}
