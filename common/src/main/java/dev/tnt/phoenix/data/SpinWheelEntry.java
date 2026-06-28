package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record SpinWheelEntry(String id, Identifier sprite) {

    public static final Codec<SpinWheelEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SpinWheelEntry::id),
            Identifier.CODEC.fieldOf("sprite").forGetter(SpinWheelEntry::sprite)
    ).apply(instance, SpinWheelEntry::new));
    public static final StreamCodec<ByteBuf, SpinWheelEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SpinWheelEntry::id,
            Identifier.STREAM_CODEC, SpinWheelEntry::sprite,
            SpinWheelEntry::new
    );
}
