package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Lock(boolean locked, LockReason reason) {

    public static final Codec<Lock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("locked").forGetter(Lock::locked),
            LockReason.CODEC.fieldOf("reason").forGetter(Lock::reason)
    ).apply(instance, Lock::new));

    public static final Lock EMPTY = new Lock(false, LockReason.NONE);
    public static final Lock SPIN = new Lock(LockReason.SPIN);

    public Lock(LockReason reason) {
        this(true, reason);
    }
}
