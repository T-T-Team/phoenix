package dev.tnt.phoenix.api;

import com.mojang.serialization.Codec;
import dev.tnt.phoenix.Phoenix;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum RiskBet implements StringRepresentable {

    NONE("none"),
    CLUBS("clubs"),
    HEARTS("hearts");

    private static final IntFunction<RiskBet> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final Codec<RiskBet> CODEC = StringRepresentable.fromEnum(RiskBet::values);
    public static final StreamCodec<ByteBuf, RiskBet> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String serializedName;

    RiskBet(String serializedName) {
        this.serializedName = serializedName;
    }

    public static RiskBet fromValue(int value) {
        int cycle = Phoenix.CONFIG.riskCycleDuration;
        return value % (2 * cycle) < cycle ? HEARTS : CLUBS;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
