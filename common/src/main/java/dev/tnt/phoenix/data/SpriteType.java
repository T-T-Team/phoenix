package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

public enum SpriteType implements StringRepresentable {

    DEFAULT("default"),
    ENABLED("enabled"),
    DISABLED("disabled");

    public static final Codec<SpriteType> CODEC = StringRepresentable.fromEnum(SpriteType::values);
    private final String serializedName;


    SpriteType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public Identifier getSpritePath(Identifier sprite) {
        return sprite.withPath(path -> path.replace(".png", this.getDefaultTexturePathSuffix() + ".png"));
    }

    public String getDefaultTexturePathSuffix() {
        return switch (this) {
            case DEFAULT -> "";
            case ENABLED -> "_on";
            case DISABLED -> "_off";
        };
    }
}
