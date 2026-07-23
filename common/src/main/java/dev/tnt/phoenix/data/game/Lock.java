package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

public record Lock(boolean locked, LockReason reason, EnumSet<LockTag> tags) {

    public static final Codec<Lock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("locked").forGetter(Lock::locked),
            LockReason.CODEC.fieldOf("reason").forGetter(Lock::reason),
            LockTag.CODEC.listOf().optionalFieldOf("lock_tags", Collections.emptyList())
                    .xmap(EnumSet::copyOf, ArrayList::new).forGetter(Lock::tags)
    ).apply(instance, Lock::new));

    public static final Lock EMPTY = new Lock(false, LockReason.NONE, EnumSet.noneOf(LockTag.class));
    public static final Lock SPIN = new Lock(LockReason.SPIN, LockTag.GAME);
    public static final Lock RISK = new Lock(LockReason.RISK, LockTag.GAME);

    public Lock(LockReason reason) {
        this(true, reason, EnumSet.noneOf(LockTag.class));
    }

    public Lock(LockReason reason, LockTag tag, LockTag... tags) {
        this(true, reason, EnumSet.of(tag, tags));
    }

    public boolean hasTag(LockTag tag) {
        return this.tags.contains(tag);
    }

    public enum LockTag implements StringRepresentable {

        GAME("game");

        public static final Codec<LockTag> CODEC = StringRepresentable.fromEnum(LockTag::values);

        private final String serializedName;

        LockTag(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
