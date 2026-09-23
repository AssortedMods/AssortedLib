package com.grim3212.assorted.lib.spawn;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A random height in a random piece of a matching structure in the column, so mineshafts and strongholds are
 * reached underground where no heightmap does. Most such spots are inside walls, so a habit using this wants
 * several tries.
 */
public record StructureSite(EntrySet<Structure> structures) implements SpawnSite {

    public static final MapCodec<StructureSite> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EntrySet.codec(Registries.STRUCTURE).fieldOf("structures").forGetter(StructureSite::structures)
    ).apply(instance, StructureSite::new));

    @Override
    public Identifier type() {
        return SpawnSites.STRUCTURE;
    }

    /** A structure's start may lie chunks away, outside a generating chunk's region; StructureManager throws for it. */
    @Override
    public boolean seedsAtGeneration() {
        return false;
    }

    @Override
    public @Nullable BlockPos find(ServerLevelAccessor level, BlockPos column, RandomSource random) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        // A generating chunk's starts are in its region, not the level yet.
        StructureManager structures = level instanceof WorldGenRegion region ? level.getLevel().structureManager().forWorldGenRegion(region) : level.getLevel().structureManager();
        // One boolean off the chunk, before startsForStructure, which resolves every reference by loading its start's chunk.
        if (!structures.hasAnyStructureAt(column)) {
            return null;
        }
        List<StructurePiece> pieces = null;
        // As StructureManager#getStructureWithPieceAt asks, for a column rather than a block.
        for (StructureStart start : structures.startsForStructure(ChunkPos.containing(column),
                structure -> registry.get(registry.getId(structure)).map(this.structures::contains).orElse(false))) {
            for (StructurePiece piece : start.getPieces()) {
                BoundingBox box = piece.getBoundingBox();
                if (box.minX() <= column.getX() && column.getX() <= box.maxX() && box.minZ() <= column.getZ() && column.getZ() <= box.maxZ()) {
                    if (pieces == null) {
                        pieces = new ArrayList<>();
                    }
                    pieces.add(piece);
                }
            }
        }
        if (pieces == null) {
            return null;
        }
        BoundingBox box = pieces.get(random.nextInt(pieces.size())).getBoundingBox();
        return new BlockPos(column.getX(), box.minY() + random.nextInt(box.maxY() - box.minY() + 1), column.getZ());
    }
}
