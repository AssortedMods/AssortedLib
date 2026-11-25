package com.grim3212.assorted.lib.client.model;

import com.grim3212.assorted.lib.client.model.loaders.context.IModelBakingContext;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("resource")
public class UnbakedGeometryHelper {

    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    private UnbakedGeometryHelper() {
        throw new IllegalStateException("Can not instantiate an instance of: UnbakedGeometryHelper. This is a utility class");
    }

    // There is no createUnbakedItemElements (the extruded "builtin/generated" item shape as
    // elements): ItemModelGenerator#processFrames is private in vanilla and common code cannot
    // widen it. A model that wants that shape parents onto minecraft:item/generated.

    /**
     * Creates {@linkplain BlockElement block elements} covering only the opaque pixels of the
     * sprite, so a flat item texture becomes a mask instead of a full quad. Only the mask: the
     * extruded edge elements are not obtainable (see above).
     */
    public static List<BlockElement> createUnbakedItemMaskElements(int layerIndex, TextureAtlasSprite sprite) {
        var elements = new ArrayList<BlockElement>();

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
                    elements.add(new BlockElement(
                            new Vector3f(16 * xStart / (float) width, 16 - 16 * yEnd / (float) height, 7.5F),
                            new Vector3f(16 * x / (float) width, 16 - 16 * y / (float) height, 8.5F),
                            Util.make(new HashMap<>(), map -> {
                                for (Direction direction : Direction.values())
                                    map.put(direction, new BlockElementFace(null, layerIndex, "#layer" + layerIndex, new BlockFaceUV(null, 0)));
                            }),
                            null,
                            true
                    ));

                    // Reset xStart
                    xStart = -1;
                }
            }
        }
        return elements;
    }

    /**
     * Turns a single {@link BlockElementFace} into a {@link BakedQuad}.
     */
    public static BakedQuad bakeElementFace(BlockElement element, BlockElementFace face, TextureAtlasSprite sprite, Direction direction, ModelState state) {
        return FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite, direction, state, element.rotation, element.shade);
    }

    /**
     * Bakes a list of {@linkplain BlockElement block elements} and feeds the baked quads to a
     * {@linkplain IModelBuilder model builder}. Each face's texture is resolved through
     * {@code owner}, as a {@code #slot} reference or a texture path.
     */
    public static void bakeElements(IModelBuilder<?> builder, List<BlockElement> elements, IModelBakingContext owner, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
        for (BlockElement element : elements) {
            for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                var side = entry.getKey();
                var face = entry.getValue();
                Material material = resolveDirtyMaterial(face.texture(), owner);
                var sprite = spriteGetter.apply(material != null ? material : missingMaterial());
                var quad = bakeElementFace(element, face, sprite, side, modelState);
                if (face.cullForDirection() == null)
                    builder.addUnculledFace(quad);
                else
                    builder.addCulledFace(Direction.rotate(modelState.getRotation().getMatrix(), face.cullForDirection()), quad);
            }
        }
    }

    /**
     * Bakes a list of {@linkplain BlockElement block elements} and returns the list of baked quads.
     */
    public static List<BakedQuad> bakeElements(List<BlockElement> elements, IModelBakingContext owner, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
        if (elements.isEmpty())
            return List.of();
        var list = new ArrayList<BakedQuad>();
        bakeElements(IModelBuilder.collecting(list), elements, owner, spriteGetter, modelState);
        return list;
    }

    private static Material missingMaterial() {
        return new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation());
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
     * Resolves a material that may name a filesystem path instead of a proper {@link ResourceLocation}.
     * A path is always taken to be on {@link InventoryMenu#BLOCK_ATLAS}; a {@code #slot}
     * reference is looked up in {@code owner}, and is null if it names nothing.
     */
    @Nullable
    public static Material resolveDirtyMaterial(@Nullable String tex, IModelBakingContext owner) {
        if (tex == null)
            return missingMaterial();
        if (tex.startsWith("#"))
            return owner.getMaterial(tex).orElse(null);

        // Attempt to convert a common (windows/linux/mac) filesystem path to a ResourceLocation.
        // This makes no promises, if it doesn't work, too bad, fix your mtl file.
        Matcher match = FILESYSTEM_PATH_TO_RESLOC.matcher(tex);
        if (match.matches()) {
            String namespace = match.group("namespace");
            String path = match.group("path").replace("\\", "/");
            tex = namespace != null ? namespace + ":" + path : path;
        }

        return new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.parse(tex));
    }
}
