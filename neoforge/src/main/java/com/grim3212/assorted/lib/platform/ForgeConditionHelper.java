package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.conditions.BlockExistsCondition;
import com.grim3212.assorted.lib.conditions.ForgeConditionProvider;
import com.grim3212.assorted.lib.conditions.LibConditions;
import com.grim3212.assorted.lib.conditions.PartEnabledCondition;
import com.grim3212.assorted.lib.conditions.TagPopulatedCondition;
import com.grim3212.assorted.lib.core.conditions.LibCondition;
import com.grim3212.assorted.lib.core.conditions.LibConditionProvider;
import com.grim3212.assorted.lib.platform.services.IConditionHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NeoForgeConditions;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ForgeConditionHelper implements IConditionHelper {

    /**
     * Nothing to do at construction: condition codecs can only be registered from a
     * {@code RegisterEvent}, so {@link LibConditions} collects them and flushes them then.
     */
    @Override
    public void init() {
    }

    @Override
    public <T> BootstrapContext<T> conditionalContext(BootstrapContext<T> context, Function<ResourceKey<T>, List<LibConditionProvider>> conditions) {
        return new ConditionalContext<>(context, conditions);
    }

    @Override
    public void register(Identifier name, LibCondition condition) {
        LibConditions.register(name, condition);
    }

    @Override
    public LibConditionProvider and(LibConditionProvider... values) {
        return wrap(NeoForgeConditions.and(unwrapAll(values)));
    }

    @Override
    public LibConditionProvider not(LibConditionProvider value) {
        return wrap(NeoForgeConditions.not(unwrap(value)));
    }

    @Override
    public LibConditionProvider or(LibConditionProvider... values) {
        return wrap(NeoForgeConditions.or(unwrapAll(values)));
    }

    @Override
    public LibConditionProvider blockExists(Identifier block) {
        return wrap(new BlockExistsCondition(block));
    }

    @Override
    public LibConditionProvider itemExists(Identifier item) {
        // NeoForge ships this one itself now, as a generic "is this key registered" condition.
        return wrap(NeoForgeConditions.itemRegistered(item));
    }

    @Override
    public LibConditionProvider blockTagExists(TagKey<Block> tag) {
        return wrap(new TagPopulatedCondition.BlockTagPopulatedCondition(tag.location()));
    }

    @Override
    public LibConditionProvider itemTagExists(TagKey<Item> tag) {
        return wrap(new TagPopulatedCondition.ItemTagPopulatedCondition(tag.location()));
    }

    @Override
    public LibConditionProvider modLoaded(String modId) {
        return wrap(NeoForgeConditions.modLoaded(modId));
    }

    @Override
    public LibConditionProvider partEnabled(String partId) {
        return wrap(new PartEnabledCondition(partId));
    }

    @Override
    public void registerPartCondition(String part, Supplier<Boolean> check) {
        LibConditions.registerPartCondition(part, check);
    }

    public static ForgeConditionProvider wrap(ICondition condition) {
        return new ForgeConditionProvider(condition);
    }

    public static ICondition unwrap(LibConditionProvider provider) {
        if (!(provider instanceof ForgeConditionProvider forgeConditionProvider))
            throw new IllegalArgumentException("The given condition is not compatible with the forge platform!");

        return forgeConditionProvider.getCondition();
    }

    private static ICondition[] unwrapAll(LibConditionProvider... providers) {
        return Arrays.stream(providers).map(ForgeConditionHelper::unwrap).toArray(ICondition[]::new);
    }

    /**
     * Adds each entry's conditions to any the caller passes. Both overloads are overridden: vanilla calls the
     * plain one, and NeoForge's default for the other drops the conditions.
     */
    private record ConditionalContext<T>(BootstrapContext<T> delegate, Function<ResourceKey<T>, List<LibConditionProvider>> conditions) implements BootstrapContext<T> {

        @Override
        public Holder.Reference<T> register(ResourceKey<T> key, T value) {
            return this.register(key, value, new ICondition[0]);
        }

        @Override
        public Holder.Reference<T> register(ResourceKey<T> key, T value, ICondition... extraConditions) {
            List<LibConditionProvider> registered = this.conditions.apply(key);
            if (registered == null || registered.isEmpty()) {
                return this.delegate.register(key, value, extraConditions);
            }

            ICondition[] all = Stream.concat(Arrays.stream(extraConditions), registered.stream().map(ForgeConditionHelper::unwrap)).toArray(ICondition[]::new);
            return this.delegate.register(key, value, all);
        }

        @Override
        public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
            return this.delegate.lookup(key);
        }

        @Deprecated
        @Override
        public <S> Stream<Holder.Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
            return this.delegate.listContextElements(key);
        }

        @Override
        public <S> Optional<HolderLookup<S>> holderLookup(ResourceKey<? extends Registry<? extends S>> registry) {
            return this.delegate.holderLookup(registry);
        }
    }
}
