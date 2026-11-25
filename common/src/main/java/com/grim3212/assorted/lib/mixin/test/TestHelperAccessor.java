package com.grim3212.assorted.lib.mixin.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The running test behind a helper, so {@code TestSupport} can hear when it ends and remove what it joined. */
@Mixin(GameTestHelper.class)
public interface TestHelperAccessor {
    @Accessor("testInfo")
    GameTestInfo assortedlib_getTestInfo();
}
