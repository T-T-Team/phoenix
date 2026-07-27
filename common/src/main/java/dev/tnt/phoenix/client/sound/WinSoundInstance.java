package dev.tnt.phoenix.client.sound;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.GameWinInfo;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class WinSoundInstance extends AbstractTickableSoundInstance {

    private final GameWinInfo winInfo;

    public WinSoundInstance(RandomSource random, GameWinInfo winInfo) {
        super(Phoenix.SOUND_WIN.get(), SoundSource.BLOCKS, random);
        this.winInfo = winInfo;
        this.looping = true;
        this.volume = 0.7F;
        this.updatePitch();
    }

    public static boolean canPlay(GameWinInfo winInfo) {
        return winInfo.isBlinkMode();
    }

    @Override
    public void tick() {
        this.updatePitch();
        if (!canPlay(this.winInfo)) {
            this.stop();
        }
    }

    private void updatePitch() {
        this.pitch = 1.0F + this.winInfo.getAnimationIndex() * 0.05F;
    }
}
