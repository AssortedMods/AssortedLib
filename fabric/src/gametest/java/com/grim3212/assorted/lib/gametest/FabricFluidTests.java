package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.core.fluid.FluidInformation;
import com.grim3212.assorted.lib.platform.FabricFluidManager;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Fabric's side of {@code IFluidManager}: a {@link FluidInformation} and a {@link FluidVariant}
 * both carry a component patch, and converting either way must keep it. NeoForge's
 * {@code FluidStack} conversion is a direct field copy.
 */
final class FabricFluidTests {

    private FabricFluidTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("fabric_fluid_variant_keeps_components", FabricFluidTests::fluidVariantKeepsComponents);
    }

    private static void fluidVariantKeepsComponents(GameTestHelper helper) {
        DataComponentPatch data = DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Marked water")).build();
        FluidInformation marked = new FluidInformation(Fluids.WATER, FluidConstants.BUCKET, data);

        FluidVariant variant = FabricFluidManager.makeVariant(marked);
        helper.assertValueEqual(variant.getComponents(), data, "the component patch on the fluid variant");
        helper.assertValueEqual(FabricFluidManager.makeInformation(variant, FluidConstants.BUCKET), marked, "the fluid information read back from the variant");

        FluidInformation flowing = new FluidInformation(Fluids.FLOWING_WATER, FluidConstants.BUCKET, data);
        FluidVariant fromFlowing = FabricFluidManager.makeVariant(flowing);
        helper.assertTrue(fromFlowing.getFluid() == Fluids.WATER, "flowing water did not become its source, it is " + fromFlowing.getFluid());
        helper.assertValueEqual(fromFlowing.getComponents(), data, "the component patch after flowing water became its source");

        helper.succeed();
    }
}
