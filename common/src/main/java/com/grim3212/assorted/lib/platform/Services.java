package com.grim3212.assorted.lib.platform;


import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.platform.services.*;

import java.util.ServiceLoader;

// Service loaders are a built-in Java feature that allow us to locate implementations of an interface that vary from one
// environment to another. In the context of MultiLoader we use this feature to access a mock API in the common code that
// is swapped out for the platform specific implementation at runtime.
public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IConfigHelper CONFIG = load(IConfigHelper.class);
    public static final IRegistryFactory REGISTRY_FACTORY = load(IRegistryFactory.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);
    public static final ILevelPropertyAccessor LEVEL_PROPERTIES = load(ILevelPropertyAccessor.class);
    public static final IEventHelper EVENTS = load(IEventHelper.class);
    public static final IFluidManager FLUIDS = load(IFluidManager.class);
    public static final IConditionHelper CONDITIONS = load(IConditionHelper.class);
    public static final IIngredientHelper INGREDIENTS = load(IIngredientHelper.class);
    public static final IWorldGenHelper WORLD_GEN = load(IWorldGenHelper.class);
    public static final IInventoryHelper INVENTORY = load(IInventoryHelper.class);

    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        LibConstants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
