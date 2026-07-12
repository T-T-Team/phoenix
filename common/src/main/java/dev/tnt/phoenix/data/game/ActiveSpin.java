package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

public final class ActiveSpin {

    public static final Codec<ActiveSpin> CODEC = MapCodec.unitCodec(ActiveSpin::new);

    public void updateFrom(ActiveSpin other) {
    }
}
