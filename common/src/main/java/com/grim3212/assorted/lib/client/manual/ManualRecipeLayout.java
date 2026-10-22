package com.grim3212.assorted.lib.client.manual;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.manual.ManualSlot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A region of a station's own container texture and where the items sit on it, so a recipe is drawn
 * on the screen that makes it. Read from
 * {@code assets/<namespace>/manual/recipe_layouts/<path>.json}, the namespace and path naming the
 * recipe type; a resource pack overrides one by writing the same path.
 * <p>
 * Positions are the ones the station's menu gives its slots, less {@link #u} and {@link #v}.
 *
 * @param columns         how many of {@code inputs} make a row, which places a smaller recipe in
 *                        the station's top left corner as the recipe book does
 * @param inputs          where each input's item is drawn, relative to the region's corner
 * @param extras          slots no recipe fills: a furnace's fuel, or the machine itself where a
 *                        recipe is drawn beside the block that runs it
 * @param cornerRadius    rounds the region's corners off; 0 leaves it square
 * @param shapelessMarker absent puts the mark in the region's top right corner
 */
public record ManualRecipeLayout(Identifier texture, int textureWidth, int textureHeight,
                                 int u, int v, int width, int height,
                                 int columns, List<Position> inputs, Position result, List<Extra> extras,
                                 int cornerRadius, Optional<Position> shapelessMarker) {

    /** Size of the shapeless mark, and its default inset from the corner. */
    public static final int MARKER = 12;
    private static final int MARKER_INSET = 3;

    /** Vanilla slot background, with the item drawn 1px in. */
    public static final int SLOT = 18;

    public static final Codec<ManualRecipeLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("texture").forGetter(ManualRecipeLayout::texture),
            Codec.INT.optionalFieldOf("texture_width", 256).forGetter(ManualRecipeLayout::textureWidth),
            Codec.INT.optionalFieldOf("texture_height", 256).forGetter(ManualRecipeLayout::textureHeight),
            Codec.INT.fieldOf("u").forGetter(ManualRecipeLayout::u),
            Codec.INT.fieldOf("v").forGetter(ManualRecipeLayout::v),
            Codec.INT.fieldOf("width").forGetter(ManualRecipeLayout::width),
            Codec.INT.fieldOf("height").forGetter(ManualRecipeLayout::height),
            Codec.INT.optionalFieldOf("columns", 1).forGetter(ManualRecipeLayout::columns),
            Position.CODEC.listOf().fieldOf("inputs").forGetter(ManualRecipeLayout::inputs),
            Position.CODEC.fieldOf("result").forGetter(ManualRecipeLayout::result),
            Extra.CODEC.listOf().optionalFieldOf("extras", List.of()).forGetter(ManualRecipeLayout::extras),
            Codec.INT.optionalFieldOf("corner_radius", 3).forGetter(ManualRecipeLayout::cornerRadius),
            Position.CODEC.optionalFieldOf("shapeless_marker").forGetter(ManualRecipeLayout::shapelessMarker)
    ).apply(instance, ManualRecipeLayout::new));

    /** Where a shapeless mark goes, relative to the corner of the region. */
    public Position shapelessMarkerPosition() {
        return this.shapelessMarker.orElseGet(() -> new Position(this.width - MARKER - MARKER_INSET, MARKER_INSET));
    }

    /** Null when the layout has no slot for it, as for a third ingredient on a two slot machine. */
    @Nullable
    public Position input(int column, int row) {
        if (column >= this.columns) {
            return null;
        }

        int index = row * this.columns + column;
        return index >= 0 && index < this.inputs.size() ? this.inputs.get(index) : null;
    }

    /** Written as a two element array. */
    public record Position(int x, int y) {

        public static final Codec<Position> CODEC = Codec.INT.listOf(2, 2)
                .xmap(values -> new Position(values.get(0), values.get(1)),
                        position -> List.of(position.x(), position.y()));
    }

    /**
     * A slot belonging to the station rather than the recipe, filled from a vanilla
     * {@link SlotDisplay}: {@code minecraft:any_fuel} or {@code minecraft:tag} (a {@code #} tag) both
     * cycle and are marked as a choice.
     *
     * @param tooltip a translation key added to the slot's tooltip, given the slot's own item name
     *                as {@code %s}. A slot the recipe does not explain itself needs one - the block
     *                doing the work, drawn beside the recipe, otherwise says nothing about what it
     *                is doing there. The wording belongs to whoever wrote the layout.
     */
    public record Extra(Position position, Dynamic<?> display, int count, Optional<String> tooltip) {

        public static final Codec<Extra> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Position.CODEC.fieldOf("position").forGetter(Extra::position),
                Codec.PASSTHROUGH.fieldOf("display").forGetter(Extra::display),
                Codec.INT.optionalFieldOf("count", 0).forGetter(Extra::count),
                Codec.STRING.optionalFieldOf("tooltip").forGetter(Extra::tooltip)
        ).apply(instance, Extra::new));

        /** Each bad display is reported once, not every frame it is drawn. */
        private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

        /** Decoded when drawn, as a tag display needs the world's tags, which resources load before. */
        public ManualSlot slot() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return ManualSlot.of(SlotDisplay.Empty.INSTANCE, this.count);
            }

            SlotDisplay display = decode(this.display, minecraft.level.registryAccess())
                    .orElse(SlotDisplay.Empty.INSTANCE);
            return ManualSlot.of(display, this.count);
        }

        private static <T> Optional<SlotDisplay> decode(Dynamic<T> display, HolderLookup.Provider registries) {
            return SlotDisplay.CODEC.parse(RegistryOps.create(display.getOps(), registries), display.getValue()).resultOrPartial(error -> {
                if (REPORTED.add(error)) {
                    LibConstants.LOG.error("Failed to read a manual recipe layout's display: {}", error);
                }
            });
        }
    }
}
