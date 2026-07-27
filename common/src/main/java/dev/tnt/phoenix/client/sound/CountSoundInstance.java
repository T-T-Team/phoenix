package dev.tnt.phoenix.client.sound;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.LockReason;
import dev.tnt.phoenix.data.component.PlayerGameInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class CountSoundInstance extends AbstractTickableSoundInstance {

    private final PlayerGameInstance gameInstance;

    public CountSoundInstance(RandomSource random, PlayerGameInstance gameInstance) {
        super(Phoenix.SOUND_COUNT.get(), SoundSource.BLOCKS, random);
        this.gameInstance = gameInstance;
        this.looping = true;
        this.volume = 0.2F;
    }

    public static boolean canPlay(PlayerGameInstance instance) {
        return instance.getLockReason().is(LockReason.TRANSFER);
    }

    @Override
    public void tick() {
        if (!canPlay(this.gameInstance)) {
            this.stop();
        }
    }
}
