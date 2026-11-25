package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.fluid.FluidInformation;
import com.grim3212.assorted.lib.core.fluid.IFluidVariantHandler;
import com.grim3212.assorted.lib.fluid.ForgeFluidVariantHandlerDelegate;
import com.grim3212.assorted.lib.platform.services.IFluidManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * An item's fluid capability is an {@link IFluidHandlerItem}, asked of a one-item copy of the stack.
 * The handler may change its item as it fills or drains (a bucket becomes another item), and
 * {@link IFluidHandlerItem#getContainer()} is what the item became, so the copy is never read back
 * directly. A simulation is {@link IFluidHandler.FluidAction#SIMULATE}.
 */
public class ForgeFluidManager implements IFluidManager {

    @Override
    public Optional<FluidInformation> get(final ItemStack stack) {
        final IFluidHandlerItem handler = handlerFor(stack);
        if (handler == null)
            return Optional.empty();

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            final FluidStack contained = handler.getFluidInTank(tank);
            if (!contained.isEmpty()) {
                return Optional.of(buildFluidInformation(contained));
            }
        }

        // The item is a fluid container, it just happens to be empty right now.
        return Optional.of(new FluidInformation(Fluids.EMPTY, 0));
    }

    @Override
    public ItemStack extractFrom(final ItemStack stack, final long amount) {
        final IFluidHandlerItem handler = handlerFor(stack);
        if (handler == null)
            return stack.copyWithCount(1);

        handler.drain(clamp(amount), IFluidHandler.FluidAction.EXECUTE);
        return handler.getContainer();
    }

    @Override
    public long simulateExtract(ItemStack stack, long amount) {
        final IFluidHandlerItem handler = handlerFor(stack);
        if (handler == null)
            return 0;

        return handler.drain(clamp(amount), IFluidHandler.FluidAction.SIMULATE).getAmount();
    }

    @Override
    public ItemStack insertInto(final ItemStack stack, final FluidInformation fluidInformation) {
        final IFluidHandlerItem handler = handlerFor(stack);
        if (handler == null)
            return stack.copyWithCount(1);

        if (clamp(fluidInformation.amount()) > 0 && fluidInformation.fluid() != Fluids.EMPTY) {
            handler.fill(buildFluidStack(fluidInformation), IFluidHandler.FluidAction.EXECUTE);
        }
        return handler.getContainer();
    }

    @Override
    public long simulateInsert(ItemStack stack, FluidInformation fluidInformation) {
        if (clamp(fluidInformation.amount()) <= 0 || fluidInformation.fluid() == Fluids.EMPTY)
            return 0;

        final IFluidHandlerItem handler = handlerFor(stack);
        if (handler == null)
            return 0;

        return handler.fill(buildFluidStack(fluidInformation), IFluidHandler.FluidAction.SIMULATE);
    }

    @Override
    public Component getDisplayName(final Fluid fluid) {
        return fluid.getFluidType().getDescription(buildFluidStack(new FluidInformation(fluid)));
    }

    @Override
    public Optional<IFluidVariantHandler> getVariantHandlerFor(Fluid fluid) {
        return Optional.of(new ForgeFluidVariantHandlerDelegate(fluid.getFluidType()));
    }

    /**
     * The fluid handler of one item of the stack, made on a copy so {@code stack} itself is never
     * touched; null when the item holds no fluid.
     */
    @Nullable
    private static IFluidHandlerItem handlerFor(final ItemStack stack) {
        if (stack.isEmpty())
            return null;

        return stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
    }

    private static int clamp(final long amount) {
        return (int) Math.max(0, Math.min(amount, Integer.MAX_VALUE));
    }

    // FluidInformation carries the same DataComponentPatch a FluidStack does, so both directions are
    // a straight copy.
    @NotNull
    public static FluidInformation buildFluidInformation(final FluidStack fluid) {
        return new FluidInformation(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
    }

    @NotNull
    public static FluidStack buildFluidStack(final FluidInformation fluid) {
        return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid.fluid()), clamp(fluid.amount()), fluid.data());
    }
}
