package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum GameType implements StringRepresentable {

    LOW("low"),
    HIGH("high");

    public static final Codec<GameType> CODEC = StringRepresentable.fromEnum(GameType::values);
    public static final IntFunction<GameType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, GameType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    private final String serializedName;

    GameType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
