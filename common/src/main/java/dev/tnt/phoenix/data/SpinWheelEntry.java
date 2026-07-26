package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class SpinWheelEntry {

    public static final Codec<SpinWheelEntry> CODEC = RecordCodecBuilder.<SpinWheelEntry>create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SpinWheelEntry::id),
            Codec.unboundedMap(SpriteType.CODEC, Identifier.CODEC).fieldOf("sprites").forGetter(t -> t.sprites),
            Options.CODEC.optionalFieldOf("sprite_options", Options.DEFAULT).forGetter(t -> t.spriteOptions)
    ).apply(instance, SpinWheelEntry::new)).validate(SpinWheelEntry::validateCodec);

    private final String id;
    private final EnumMap<SpriteType, Identifier> sprites;
    private final Options spriteOptions;

    public SpinWheelEntry(String id, Map<SpriteType, Identifier> sprites, Options spriteOptions) {
        this.id = id;
        this.sprites = new EnumMap<>(SpriteType.class);
        this.sprites.putAll(sprites);
        this.spriteOptions = spriteOptions;
    }

    public String id() {
        return this.id;
    }

    public Identifier getSpriteForType(SpriteType type) {
        return this.sprites.computeIfAbsent(type, _ -> this.sprites.get(SpriteType.DEFAULT));
    }

    public Options getSpriteOptions() {
        return spriteOptions;
    }

    private static DataResult<SpinWheelEntry> validateCodec(SpinWheelEntry entry) {
        if (!entry.sprites.containsKey(SpriteType.DEFAULT)) {
            return DataResult.error(() -> "Sprite map must contain 'default' element");
        }
        return DataResult.success(entry);
    }

    public record Options(boolean useDefaultTextureOnly) {

        public static final Codec<Options> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("default_texture_only", false).forGetter(Options::useDefaultTextureOnly)
        ).apply(instance, Options::new));
        public static final Options DEFAULT = new Options(false);
    }
}
