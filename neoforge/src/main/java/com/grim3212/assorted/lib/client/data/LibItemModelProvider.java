package com.grim3212.assorted.lib.client.data;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.manual.LibItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** The library registers no blocks, so this only has the manual to model. */
public class LibItemModelProvider extends ItemModelProvider {

    public LibItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, LibConstants.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Assorted Lib item models";
    }

    @Override
    protected void registerModels() {
        this.basicItem(LibItems.INSTRUCTION_MANUAL.get());
    }
}
