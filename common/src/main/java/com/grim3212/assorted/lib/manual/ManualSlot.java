package com.grim3212.assorted.lib.manual;

import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * One recipe slot, held as a {@link SlotDisplay} rather than as resolved stacks so the tag or
 * {@code AnyFuel} behind it survives for the tooltip.
 *
 * @param count stack size to override with, or 0 to keep what the display gives
 */
public record ManualSlot(SlotDisplay display, int count) {

    public static final ManualSlot EMPTY = new ManualSlot(SlotDisplay.Empty.INSTANCE, 0);

    public static ManualSlot of(SlotDisplay display) {
        return new ManualSlot(display, 0);
    }

    public static ManualSlot of(SlotDisplay display, int count) {
        return new ManualSlot(display, count);
    }

    /** Every stack this slot accepts, in cycling order. */
    public List<ItemStack> stacks(ContextMap context) {
        List<ItemStack> stacks = this.display.resolveForStacks(context);
        if (this.count <= 0) {
            return stacks;
        }

        return stacks.stream().map(stack -> stack.copyWithCount(this.count)).toList();
    }

    /**
     * The tags this slot will take anything from, for the note in its tooltip. More than one only
     * happens for a slot built out of several displays.
     */
    public List<TagKey<Item>> tags() {
        List<TagKey<Item>> tags = new ArrayList<>();
        collectTags(this.display, tags);
        return List.copyOf(tags);
    }

    /** Whether the slot takes any one of several things, which is what the book marks in gold. */
    public boolean isChoice(ContextMap context) {
        return this.display instanceof SlotDisplay.AnyFuel || !this.tags().isEmpty() || this.stacks(context).size() > 1;
    }

    public boolean isEmpty() {
        return this.display instanceof SlotDisplay.Empty;
    }

    private static void collectTags(SlotDisplay display, List<TagKey<Item>> out) {
        switch (display) {
            case SlotDisplay.TagSlotDisplay tag -> tag.tag().unwrapKey().ifPresent(out::add);
            // A composite is how several ingredients end up in one slot; each half may be a tag.
            case SlotDisplay.Composite composite -> composite.contents().forEach(part -> collectTags(part, out));
            case SlotDisplay.WithRemainder remainder -> collectTags(remainder.input(), out);
            default -> {
            }
        }
    }
}
