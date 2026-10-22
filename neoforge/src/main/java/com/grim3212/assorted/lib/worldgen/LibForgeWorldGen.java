package com.grim3212.assorted.lib.worldgen;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.platform.ForgeWorldGenHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

public class LibForgeWorldGen {

    public static void init(IEventBus modEventBus) {
        modEventBus.<RegisterEvent>addListener(event -> {
            event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, registry -> {
                registry.register(Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "lib_biome_modifier"), ForgeBiomeModifier.CODEC);
            });
        });
    }

    private static class ForgeBiomeModifier implements BiomeModifier {

        private static final ForgeBiomeModifier INSTANCE = new ForgeBiomeModifier();

        /**
         * A biome modifier is serialised by a {@link MapCodec} now rather than a plain {@link
         * com.mojang.serialization.Codec}, so the codec can be a constant again instead of being
         * captured while registering.
         */
        private static final MapCodec<ForgeBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            ForgeWorldGenHelper.modifyBiome(biome, phase, builder);
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return CODEC;
        }

    }
}
