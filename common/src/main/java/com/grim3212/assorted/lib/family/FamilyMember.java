package com.grim3212.assorted.lib.family;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** One mod's place in its family, returned by {@link Families#join} to add an icon or the manual order. */
public final class FamilyMember {

    private final String modId;
    private final String familyId;
    @Nullable
    private volatile ResourceLocation icon;
    private volatile int iconWeight;

    FamilyMember(String modId, String familyId) {
        this.modId = modId;
        this.familyId = familyId;
    }

    public String modId() {
        return this.modId;
    }

    public String familyId() {
        return this.familyId;
    }

    /**
     * Offers {@code item} as the family's icon, for its creative tab, manual section and advancement root. Of the
     * installed members that are on, the highest weight is drawn; ties go to the lower mod id.
     */
    public FamilyMember icon(ResourceLocation item, int weight) {
        this.icon = item;
        this.iconWeight = weight;
        return this;
    }

    /** Where the family's manual section sorts in the index; lower comes first. Every member should give the same. */
    public FamilyMember manualOrder(int order) {
        Families.manualOrder(this, order);
        return this;
    }

    @Nullable
    ResourceLocation icon() {
        return this.icon;
    }

    int iconWeight() {
        return this.iconWeight;
    }
}
