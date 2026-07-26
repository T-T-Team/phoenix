package dev.tnt.phoenix.client.sound;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class SpinRollSoundInstance extends AbstractTickableSoundInstance {

    private final PlayerGameInstance gameInstance;

    public SpinRollSoundInstance(RandomSource random, PlayerGameInstance instance) {
        super(Phoenix.SOUND_ROLL.get(), SoundSource.BLOCKS, random);
        this.looping = true;
        this.gameInstance = instance;
    }

    @Override
    public void tick() {
        if (!this.gameInstance.isRolling()) {
            this.stop();
        }
    }
}
