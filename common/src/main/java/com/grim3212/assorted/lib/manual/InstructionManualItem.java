package com.grim3212.assorted.lib.manual;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.item.LibDataComponents;
import com.grim3212.assorted.lib.client.manual.ManualClient;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.dist.DistExecutor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The book. Right clicking opens it, at the page of whatever was clicked when that has one.
 */
public class InstructionManualItem extends Item implements IManualEntry.IManualItem {

    /** Its own page, for when one is drawn on a page and clicked. */
    private static final ManualPageRef OWN_PAGE = ManualPageRef.of(LibConstants.MOD_ID, "reading", "crafting");

    public InstructionManualItem(Properties properties) {
        super(properties.stacksTo(1).component(LibDataComponents.MANUAL_SECTIONS.get(), ManualSectionsTooltip.INSTANCE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return openLookedAt(context.getLevel());
    }

    /** Whatever the crosshair is on that did not already open the book above. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return new InteractionResultHolder<>(openLookedAt(level), player.getItemInHand(hand));
    }

    @Override
    public ManualPageRef getManualPage(ItemStack stack) {
        return OWN_PAGE;
    }

    private static InteractionResult openLookedAt(Level level) {
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ManualClient::openAtLookedAt);
        }

        return InteractionResult.SUCCESS;
    }
}
