package dev.tnt.phoenix.data;

import net.minecraft.resources.Identifier;

public enum SpriteType {

    DEFAULT(""),
    ENABLED("_on"),
    DISABLED("_off");

    private final String pathSuffix;

    SpriteType(String pathSuffix) {
        this.pathSuffix = pathSuffix;
    }

    public Identifier getPath(Identifier basePath) {
        return basePath.withPath(path -> "textures/" + path + this.pathSuffix + ".png");
    }
}
