package dev.tnt.phoenix.data.sequence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.function.Consumer;

public record SimpleSequenceGenerator(List<String> sequence) implements SequenceGenerator {

    public static final MapCodec<SimpleSequenceGenerator> CODEC = Codec.STRING.listOf()
            .xmap(SimpleSequenceGenerator::new, SimpleSequenceGenerator::sequence)
            .fieldOf("sequence");

    @Override
    public void generateSymbolSequence(RandomSource random, Consumer<String> output) {
        this.sequence.forEach(output);
    }

    @Override
    public Type type() {
        return Type.SIMPLE;
    }
}
