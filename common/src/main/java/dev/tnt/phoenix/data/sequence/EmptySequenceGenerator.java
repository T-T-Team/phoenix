package dev.tnt.phoenix.data.sequence;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;

import java.util.function.Consumer;

public record EmptySequenceGenerator() implements SequenceGenerator {

    public static final EmptySequenceGenerator INSTANCE = new EmptySequenceGenerator();
    public static final MapCodec<EmptySequenceGenerator> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public void generateSymbolSequence(RandomSource random, Consumer<String> output) {
    }

    @Override
    public Type type() {
        return Type.EMPTY;
    }
}
