package dev.tnt.phoenix.client.sound;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.SpinGame;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class SpinRollSoundInstance extends AbstractTickableSoundInstance {

    private final SpinGame gameInstance;

    public SpinRollSoundInstance(RandomSource random, SpinGame gameInstance) {
        super(Phoenix.SOUND_ROLL.get(), SoundSource.BLOCKS, random);
        this.looping = true;
        this.gameInstance = gameInstance;
        this.volume = 0.5F;
    }

    public static boolean canPlay(SpinGame game) {
        return game.isRolling();
    }

    @Override
    public void tick() {
        if (!canPlay(this.gameInstance)) {
            this.stop();
        }
    }
}
