package dev.tnt.phoenix.client.sound;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.RiskGame;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class RiskSoundInstance extends AbstractTickableSoundInstance {

    private final RiskGame riskGame;

    public RiskSoundInstance(RandomSource random, RiskGame riskGame) {
        super(Phoenix.SOUND_GAMBLE.get(), SoundSource.BLOCKS, random);
        this.riskGame = riskGame;
        this.looping = true;
        this.volume = 0.4F;
    }

    public static boolean canPlay(RiskGame riskGame) {
        return riskGame.isActive() && !riskGame.isStopped();
    }

    @Override
    public void tick() {
        if (!canPlay(this.riskGame)) {
            this.stop();
        }
    }
}
