package dev.tnt.phoenix.data.sequence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.function.Consumer;

public interface SequenceGenerator {

    Codec<SequenceGenerator> CODEC = Type.CODEC.dispatch("generator", SequenceGenerator::type, Type::codec);

    List<String> listAvailableSymbols();

    void generateSymbolSequence(RandomSource random, Consumer<String> output);

    Type type();

    enum Type implements StringRepresentable {

        EMPTY("empty", EmptySequenceGenerator.CODEC),
        SIMPLE("simple_sequence", SimpleSequenceGenerator.CODEC),
        WEIGHTED_POOLS("weighted_pools", WeightedPoolSequenceGenerator.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        private final String serializedName;
        private final MapCodec<? extends SequenceGenerator> codec;

        Type(String serializedName, MapCodec<? extends SequenceGenerator> codec) {
            this.serializedName = serializedName;
            this.codec = codec;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }

        public MapCodec<? extends SequenceGenerator> codec() {
            return this.codec;
        }
    }
}
