package dev.tnt.phoenix.data.sequence;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public record EmptySequenceGenerator() implements SequenceGenerator {

    public static final EmptySequenceGenerator INSTANCE = new EmptySequenceGenerator();
    public static final MapCodec<EmptySequenceGenerator> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public List<String> listAvailableSymbols() {
        return Collections.emptyList();
    }

    @Override
    public void generateSymbolSequence(RandomSource random, Consumer<String> output) {
    }

    @Override
    public Type type() {
        return Type.EMPTY;
    }
}
