package com.grim3212.assorted.lib.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
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
        BakedModel missing = client.getModelManager().getMissingModel();

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!namespace.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                // Blocks drawn by their block entity or not at all have no block model to check.
                if (state.getRenderShape() != RenderShape.MODEL) {
                    continue;
                }
                BakedModel model = client.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
                if (model == missing) {
                    problems.add(state + " has no model");
                } else if (isMissing(particle(model)) || drawsMissingTexture(model, state)) {
                    problems.add(state + " draws the missing texture");
                }
            }
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (!namespace.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            // Resolves the stack's overrides, so an item drawn from its components is checked as it draws.
            BakedModel model = client.getItemRenderer().getModel(stack, client.level, null, 0);
            if (model == missing) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " has no item model");
            } else if (isMissing(particle(model))) {
                problems.add(BuiltInRegistries.ITEM.getKey(item) + " draws the missing texture");
            }
        }

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!namespace.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
                continue;
            }
            String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            try {
                Entity entity = type.create(client.level);
                if (entity == null) {
                    continue;
                }
                EntityRenderer<? super Entity> renderer = client.getEntityRenderDispatcher().getRenderer(entity);
                if (renderer == null) {
                    problems.add(name + " has no renderer, which crashes the client when one comes into view");
                } else {
                    renderer.getTextureLocation(entity);
                }
            } catch (RuntimeException e) {
                problems.add(name + " threw while being readied to draw: " + e);
            }
        }

        return problems;
    }

    // NeoForge deprecates the vanilla overload for its ModelData one, which the vanilla jar this module builds against lacks.
    @SuppressWarnings("deprecation")
    private static TextureAtlasSprite particle(BakedModel model) {
        return model.getParticleIcon();
    }

    // As above: NeoForge's getQuads overload takes ModelData and a RenderType, neither of which exists in the vanilla jar.
    @SuppressWarnings("deprecation")
    private static boolean drawsMissingTexture(BakedModel model, BlockState state) {
        // A null direction is the quads that are never culled.
        List<Direction> sides = Stream.concat(Arrays.stream(Direction.values()), Stream.of((Direction) null)).toList();
        return sides.stream()
                .flatMap(side -> model.getQuads(state, side, RandomSource.create(0L)).stream())
                .map(BakedQuad::getSprite)
                .anyMatch(ClientModelCheck::isMissing);
    }

    private static boolean isMissing(TextureAtlasSprite sprite) {
        return sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
    }
}
