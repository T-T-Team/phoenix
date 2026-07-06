package dev.tnt.phoenix.data.sequence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public record WeightedPoolSequenceGenerator(List<Pool> pools) implements SequenceGenerator {

    public static final MapCodec<WeightedPoolSequenceGenerator> CODEC = Pool.CODEC.listOf()
            .xmap(WeightedPoolSequenceGenerator::new, WeightedPoolSequenceGenerator::pools)
            .fieldOf("pools");

    @Override
    public void generateSymbolSequence(RandomSource random, Consumer<String> output) {
        Generator generator = new Generator(this.pools);
        while (generator.canGenerate()) {
            int poolIndex = generator.getPoolIndex(random);
            PoolUsageTracker tracker = generator.getPoolTracker(poolIndex);
            String symbol = tracker.source.symbol();
            output.accept(symbol);
            tracker.remaining--;
            if (tracker.remaining == 0)
                generator.remove(poolIndex);
        }
    }

    @Override
    public Type type() {
        return Type.WEIGHTED_POOLS;
    }

    public record Pool(String symbol, int count, int weight) {

        public static final Codec<Pool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("symbol").forGetter(Pool::symbol),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(Pool::count),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Pool::weight)
        ).apply(instance, Pool::new));
    }

    private static final class PoolUsageTracker {

        private final Pool source;
        private int remaining;

        public PoolUsageTracker(Pool source) {
            this.source = source;
            this.remaining = source.count;
        }
    }

    private static final class Generator {

        private final List<PoolUsageTracker> pools;
        private int totalWeight;

        public Generator(List<Pool> pools) {
            this.pools = pools.stream()
                    .map(PoolUsageTracker::new)
                    .collect(Collectors.toList());
            this.recalculateWeight();
        }

        public boolean canGenerate() {
            return !pools.isEmpty();
        }

        public int getPoolIndex(RandomSource random) {
            int value = random.nextInt(this.totalWeight);
            for (int i = this.pools.size() - 1; i >= 0; i--) {
                PoolUsageTracker pool = this.pools.get(i);
                value -= pool.source.weight;
                if (value < 0) {
                    return i;
                }
            }
            throw new IllegalStateException("No pool found");
        }

        public PoolUsageTracker getPoolTracker(int index) {
            return this.pools.get(index);
        }

        public void remove(int index) {
            this.pools.remove(index);
            this.recalculateWeight();
        }

        private void recalculateWeight() {
            this.totalWeight = this.pools.stream()
                    .mapToInt(v -> v.source.weight)
                    .sum();
        }
    }
}
