package com.grim3212.assorted.lib.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** What a mod's blocks, items and entities draw as on a real client, where a model file that exists can still fail to bake or name a missing texture. */
public final class ClientModelCheck {

    private ClientModelCheck() {
    }

    /** Every block state, item and entity in the namespace that would draw wrong or not at all; call on the client thread in a world. */
    public static List<String> problems(String namespace) {
        Minecraft client = Minecraft.getInstance();
        List<String> problems = new ArrayList<>();

        BlockStateModelSet models = client.getModelManager().getBlockStateModelSet();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!namespace.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                // Blocks drawn by their block entity or not at all have no block model to check.
                if (state.getRenderShape() != RenderShape.MODEL) {
                    continue;
                }
                BlockStateModel model = models.get(state);
                if (model == models.missingModel()) {
                    problems.add(state + " has no model");
                } else if (isMissing(model.particleMaterial()) || drawsMissingTexture(model)) {
                    problems.add(state + " draws the missing texture");
                }
            }
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (!namespace.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            Identifier modelId = stack.get(DataComponents.ITEM_MODEL);
            if (modelId == null || client.getModelManager().getItemModel(modelId) instanceof MissingItemModel) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " has no item model");
                continue;
            }
            ItemStackRenderState state = new ItemStackRenderState();
            client.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, client.level, null, 0);
            if (state.isEmpty() || isMissing(state.pickParticleMaterial(RandomSource.create(0L)))) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " draws the missing texture");
            }
        }

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!namespace.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
                continue;
            }
            String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            try {
                Entity entity = type.create(client.level, EntitySpawnReason.LOAD);
                if (entity == null) {
                    continue;
                }
                // Never added to the level, so it is given an id of its own the way spawning would.
                entity.setId(-1000 - BuiltInRegistries.ENTITY_TYPE.getId(type));
                EntityRenderer<? super Entity, ?> renderer = client.getEntityRenderDispatcher().getRenderer(entity);
                if (renderer == null) {
                    problems.add(name + " has no renderer, which crashes the client when one comes into view");
                } else {
                    renderer.createRenderState(entity, 0.0F);
                }
            } catch (RuntimeException e) {
                problems.add(name + " threw while being readied to draw: " + e);
            }
        }

        return problems;
    }

    private static boolean drawsMissingTexture(BlockStateModel model) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        // A null direction is the quads that are never culled.
        List<Direction> sides = Stream.concat(Arrays.stream(Direction.values()), Stream.of((Direction) null)).toList();
        return parts.stream().anyMatch(part -> sides.stream()
                .flatMap(side -> part.getQuads(side).stream())
                .map(BakedQuad::materialInfo)
                .anyMatch(info -> isMissing(info.sprite())));
    }

    private static boolean isMissing(Material.Baked material) {
        return material == null || isMissing(material.sprite());
    }

    private static boolean isMissing(TextureAtlasSprite sprite) {
        return MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
    }
}
