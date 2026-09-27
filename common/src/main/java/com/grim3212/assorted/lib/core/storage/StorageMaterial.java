package com.grim3212.assorted.lib.core.storage;

import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import java.util.Optional;

/** What a material container is made of, which sets its size and how far up the upgrades it is. */
public enum StorageMaterial implements StringRepresentable {
    // Vanilla materials
    STONE("stone", () -> LibCommonTags.Items.STONE, "minecraft:block/stone", 0, 3, 9, 1, 5, () -> Block.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3.5F).requiresCorrectToolForDrops()),
    COPPER("copper", () -> LibCommonTags.Items.INGOTS_COPPER, "minecraft:block/copper_block", 1, 4, 9, 1, 6, () -> Block.Properties.of().mapColor(MapColor.COLOR_ORANGE).sound(SoundType.COPPER).requiresCorrectToolForDrops().strength(3.0F, 6.0F)),
    IRON("iron", () -> LibCommonTags.Items.INGOTS_IRON, "minecraft:block/iron_block", 1, 5, 9, 1, 8, 5.0F, 6.0F),
    AMETHYST("amethyst", () -> LibCommonTags.Items.GEMS_AMETHYST, "minecraft:block/amethyst_block", 2, 6, 9, 1, 7, () -> Block.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.AMETHYST).requiresCorrectToolForDrops().strength(1.5F)),
    EMERALD("emerald", () -> LibCommonTags.Items.GEMS_EMERALD, "minecraft:block/emerald_block", 2, 7, 9, 1, 9, () -> Block.Properties.of().mapColor(MapColor.EMERALD).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F)),
    GOLD("gold", () -> LibCommonTags.Items.INGOTS_GOLD, "minecraft:block/gold_block", 3, 8, 9, 2, 5, () -> Block.Properties.of().mapColor(MapColor.GOLD).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(3.0F, 6.0F)),
    DIAMOND("diamond", () -> LibCommonTags.Items.GEMS_DIAMOND, "minecraft:block/diamond_block", 4, 9, 11, 3, 5, () -> Block.Properties.of().mapColor(MapColor.DIAMOND).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F)),
    NETHERITE("netherite", () -> LibCommonTags.Items.INGOTS_NETHERITE, "minecraft:block/netherite_block", 5, 9, 14, 3, 9, () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().strength(50.0F, 1200.0F)),

    // Assorted Core added materials
    ALUMINUM("aluminum", () -> LibCommonTags.Items.INGOTS_ALUMINUM, "block/particle/aluminum_block", 1, 4, 9, 1, 6, 5.0F, 6.0F),
    TIN("tin", () -> LibCommonTags.Items.INGOTS_TIN, "block/particle/tin_block", 1, 4, 10, 1, 6, 5.0F, 6.0F),
    TOPAZ("topaz", () -> LibCommonTags.Items.GEMS_TOPAZ, "block/particle/topaz_block", 1, 5, 9, 1, 7, () -> Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F)),
    PERIDOT("peridot", () -> LibCommonTags.Items.GEMS_PERIDOT, "block/particle/peridot_block", 2, 6, 9, 1, 7, () -> Block.Properties.of().mapColor(MapColor.COLOR_GREEN).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F)),
    LEAD("lead", () -> LibCommonTags.Items.INGOTS_LEAD, "block/particle/lead_block", 2, 6, 9, 1, 8, 5.0F, 6.0F),
    NICKEL("nickel", () -> LibCommonTags.Items.INGOTS_NICKEL, "block/particle/nickel_block", 2, 6, 10, 1, 8, 5.0F, 6.0F),
    SAPPHIRE("sapphire", () -> LibCommonTags.Items.GEMS_SAPPHIRE, "block/particle/sapphire_block", 2, 7, 9, 1, 9, () -> Block.Properties.of().mapColor(MapColor.COLOR_BLUE).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F)),
    BRONZE("bronze", () -> LibCommonTags.Items.INGOTS_BRONZE, "block/particle/bronze_block", 3, 9, 9, 2, 6, 5.0F, 6.0F),
    RUBY("ruby", () -> LibCommonTags.Items.GEMS_RUBY, "block/particle/ruby_block", 3, 9, 10, 2, 6, () -> Block.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F)),
    INVAR("invar", () -> LibCommonTags.Items.INGOTS_INVAR, "block/particle/invar_block", 4, 9, 10, 2, 8, 5.0F, 6.0F),
    SILVER("silver", () -> LibCommonTags.Items.INGOTS_SILVER, "block/particle/silver_block", 3, 9, 11, 2, 7, 5.0F, 6.0F),
    ELECTRUM("electrum", () -> LibCommonTags.Items.INGOTS_ELECTRUM, "block/particle/electrum_block", 4, 9, 11, 3, 5, 5.0F, 6.0F),
    STEEL("steel", () -> LibCommonTags.Items.INGOTS_STEEL, "block/particle/steel_block", 4, 9, 12, 3, 6, 5.0F, 6.0F),
    PLATINUM("platinum", () -> LibCommonTags.Items.INGOTS_PLATINUM, "block/particle/platinum_block", 5, 9, 13, 3, 8, 5.0F, 6.0F);

    /** A locked block's menu data: the material its client-side menu is sized from, if any. */
    public static final StreamCodec<ByteBuf, Optional<StorageMaterial>> OPTIONAL_STREAM_CODEC = ByteBufCodecs.optional(ByteBufCodecs.idMapper(i -> values()[i], StorageMaterial::ordinal));

    private final String name;
    private final Supplier<TagKey<Item>> material;
    /** A full id for a vanilla texture, or a path in the namespace of the mod that asks for it. */
    private final String particle;
    private final Supplier<Block.Properties> props;
    /** Where this material ranks against the others: wood and stone worst, netherite best. */
    private final int storageLevel;
    private final int xRows;
    private final int yCols;

    private final int hopperXRows;
    private final int hopperYCols;

    private StorageMaterial(String name, Supplier<TagKey<Item>> material, String particle, int storageLevel, int xRows, int yCols, int hopperXRows, int hopperYCols, float destroyTime, float explosionResistance) {
        this(name, material, particle, storageLevel, xRows, yCols, hopperXRows, hopperYCols, () -> Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(destroyTime, explosionResistance));
    }

    private StorageMaterial(String name, Supplier<TagKey<Item>> material, String particle, int storageLevel, int xRows, int yCols, int hopperXRows, int hopperYCols, Supplier<Block.Properties> props) {
        this.name = name;
        this.material = material;
        this.particle = particle;
        this.storageLevel = storageLevel;
        this.xRows = xRows;
        this.yCols = yCols;
        this.hopperXRows = hopperXRows;
        this.hopperYCols = hopperYCols;
        this.props = props;
    }

    /**
     * The particle texture. The added metals and gems have no vanilla block to borrow one from, so
     * theirs lives in {@code namespace}, the mod that ships it.
     */
    public Identifier getParticle(String namespace) {
        return this.particle.contains(":") ? Identifier.parse(this.particle) : Identifier.fromNamespaceAndPath(namespace, this.particle);
    }

    public int getStorageLevel() {
        return storageLevel;
    }

    public int getXRows() {
        return xRows;
    }

    public int getYCols() {
        return yCols;
    }

    public int totalItems() {
        return this.xRows * this.yCols;
    }

    public int hopperXRows() {
        return this.hopperXRows;
    }

    public int hopperYCols() {
        return this.hopperYCols;
    }

    public int hopperSize() {
        return this.hopperXRows * this.hopperYCols;
    }

    public int hopperCooldown() {
        return 8 - storageLevel;
    }

    public TagKey<Item> getMaterial() {
        return material.get();
    }

    public Block.Properties getProps() {
        return props.get();
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

}
