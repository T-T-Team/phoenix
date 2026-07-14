package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;

public final class SpinWheelEntry {

    public static final Codec<SpinWheelEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SpinWheelEntry::id),
            Identifier.CODEC.fieldOf("sprite").forGetter(t -> t.path),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(SpinWheelEntry::hidden)
    ).apply(instance, SpinWheelEntry::new));
    public static final StreamCodec<ByteBuf, SpinWheelEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SpinWheelEntry::id,
            Identifier.STREAM_CODEC, t -> t.path,
            ByteBufCodecs.BOOL, SpinWheelEntry::hidden,
            SpinWheelEntry::new
    );

    private final String id;
    private final Identifier path;
    private final boolean hidden;
    private final EnumMap<SpriteType, Identifier> sprites = new EnumMap<>(SpriteType.class);

    public SpinWheelEntry(String id, Identifier sprite, boolean hidden) {
        this.id = id;
        this.path = sprite;
        this.hidden = hidden;
    }

    public String id() {
        return this.id;
    }

    public Identifier getSpriteForType(SpriteType type) {
        return this.sprites.computeIfAbsent(type, t -> t.getPath(this.path));
    }

    public boolean hidden() {
        return this.hidden;
    }

    public boolean visible() {
        return !hidden;
    }
}
