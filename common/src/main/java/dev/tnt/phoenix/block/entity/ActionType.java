package dev.tnt.phoenix.block.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

public enum ActionType {

    PLAY,
    RISK_HEARTS,
    RISK_CLUBS,
    HOLD_1,
    HOLD_2,
    HOLD_3,
    BET,
    ADVANCED,
    MULTIWIN,
    PAYOUT;

    public static final IntFunction<ActionType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, ActionType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    public static ActionType holdActionFromIndex(int index) {
        return switch (index) {
            case 0 -> HOLD_1;
            case 1 -> HOLD_2;
            case 2 -> HOLD_3;
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
    }
}
