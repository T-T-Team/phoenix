package dev.tnt.phoenix.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NonNull;

public record Lock(boolean locked, LockReason reason) {

    public static final Codec<Lock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("locked").forGetter(Lock::locked),
            LockReason.CODEC.fieldOf("reason").forGetter(Lock::reason)
    ).apply(instance, Lock::new));

    public static final Lock EMPTY = new Lock(false, LockReason.NONE);

    public static Lock create(LockReason reason) {
        return new Lock(true, reason);
    }

    @Override
    public @NonNull String toString() {
        return this.locked ? "Lock[" + reason + "]" : "<No Lock>";
    }
}
