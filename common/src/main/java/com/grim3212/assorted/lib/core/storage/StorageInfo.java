package com.grim3212.assorted.lib.core.storage;

import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.grim3212.assorted.lib.core.item.SharedDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A storage item's tooltip lines, its lock and the storage level it has or upgrades to. The lock
 * itself stays in {@code custom_data}, where {@link StorageUtil} keeps it.
 *
 * @param lock  how the lock is shown, when the stack has one
 * @param level the storage level to show, or -1 for none
 */
public record StorageInfo(LockLine lock, int level) implements TooltipProvider {

    public static final Codec<StorageInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LockLine.CODEC.fieldOf("lock").forGetter(StorageInfo::lock),
            Codec.INT.optionalFieldOf("level", -1).forGetter(StorageInfo::level)
    ).apply(instance, StorageInfo::new));
    public static final StreamCodec<ByteBuf, StorageInfo> STREAM_CODEC = StreamCodec.composite(
            LockLine.STREAM_CODEC, StorageInfo::lock,
            ByteBufCodecs.VAR_INT, StorageInfo::level,
            StorageInfo::new);

    /** The shared component type under {@code id}; every storage mod passes the same id and gets the same type. */
    public static Supplier<DataComponentType<StorageInfo>> type(Identifier id) {
        return SharedDataComponents.tooltipComponent(id, () -> new DataComponentType.Builder<StorageInfo>().persistent(CODEC).networkSynchronized(STREAM_CODEC).build());
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, DataComponentGetter components) {
        String code = StorageUtil.getCode(components);
        if (!code.isEmpty()) {
            switch (this.lock) {
                case LOCKED -> tooltip.accept(Component.translatable("assortedlib.info.locked").withStyle(ChatFormatting.AQUA));
                case CODE -> tooltip.accept(Component.translatable("assortedlib.info.combo", Component.literal(code).withStyle(ChatFormatting.AQUA)));
                case NONE -> {
                }
            }
        }

        if (this.level >= 0) {
            tooltip.accept(Component.translatable("assortedlib.info.storage_level", Component.literal("" + this.level).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        }
    }

    public enum LockLine implements StringRepresentable {
        /** Shows nothing, even when locked. */
        NONE("none"),
        /** Says the stack is locked without giving the code away. */
        LOCKED("locked"),
        /** Shows the code. */
        CODE("code");

        public static final Codec<LockLine> CODEC = StringRepresentable.fromEnum(LockLine::values);
        public static final StreamCodec<ByteBuf, LockLine> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], LockLine::ordinal);

        private final String name;

        LockLine(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
