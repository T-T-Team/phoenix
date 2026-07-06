package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public final class SlotMachineConfig {

    public static final Codec<SlotMachineConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpinWheelEntry.CODEC.listOf().fieldOf("entries").forGetter(c -> new ArrayList<>(c.entries.values())),
            Codec.unboundedMap(GameType.CODEC, Sequence.CODEC.listOf()).fieldOf("sequences").forGetter(c -> c.sequences),
            Codec.unboundedMap(GameType.CODEC, WinConfiguration.CODEC).fieldOf("win_configuration").forGetter(c -> c.winConfiguration)
    ).apply(instance, SlotMachineConfig::new));
    public static final StreamCodec<FriendlyByteBuf, SlotMachineConfig> STREAM_CODEC = StreamCodec.composite(
            SpinWheelEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), c -> new ArrayList<>(c.entries.values()),
            ByteBufCodecs.map(HashMap::new, GameType.STREAM_CODEC, Sequence.STREAM_CODEC.apply(ByteBufCodecs.list())), c -> c.sequences,
            ByteBufCodecs.map(HashMap::new, GameType.STREAM_CODEC, WinConfiguration.STREAM_CODEC), cfg -> cfg.winConfiguration,
            SlotMachineConfig::new
    );

    private final Map<String, SpinWheelEntry> entries;
    private final Map<GameType, List<Sequence>> sequences;
    private final Map<GameType, WinConfiguration> winConfiguration;

    private SlotMachineConfig(List<SpinWheelEntry> entries, Map<GameType, List<Sequence>> sequences, Map<GameType, WinConfiguration> winConfiguration) {
        this.entries = Util.make(new HashMap<>(), map -> entries.forEach(entry -> map.put(entry.id(), entry)));
        this.sequences = sequences;
        this.winConfiguration = winConfiguration;
    }

    public Identifier getSprite(String symbol) {
        SpinWheelEntry entry = Objects.requireNonNull(this.entries.get(symbol), "No sprite defined for symbol: " + symbol);
        return entry.texturePath();
    }

    public List<Identifier> generateSequence(RandomSource random, GameType gameType, int sequenceIndex) {
        Sequence sequence = this.sequences.get(gameType).get(sequenceIndex);
        return sequence.generate(random, this);
    }

    @Deprecated
    public List<Identifier> getSprites() {
        return this.entries.values().stream()
                .filter(SpinWheelEntry::visible)
                .map(SpinWheelEntry::texturePath)
                .toList();
    }

    public WinConfiguration getWinningConfiguration(GameType gameType) {
        return this.winConfiguration.get(gameType);
    }

    private record Sequence(List<SequencePool> sequence) {
        public static final Codec<Sequence> CODEC = SequencePool.CODEC.listOf()
                .xmap(Sequence::new, Sequence::sequence);
        public static final StreamCodec<ByteBuf, Sequence> STREAM_CODEC = StreamCodec.composite(
                SequencePool.STREAM_CODEC.apply(ByteBufCodecs.list()), Sequence::sequence,
                Sequence::new
        );

        public List<Identifier> generate(RandomSource random, SlotMachineConfig source) {
            WeightedGenerator generator = new WeightedGenerator(this.initGenerator());
            List<Identifier> output = new ArrayList<>();
            while (generator.canGenerate()) {
                int poolIndex = generator.getPoolIndex(random);
                TrackedSequencePool pool = generator.getPool(poolIndex);
                String symbol = pool.source.symbol();
                Identifier sprite = source.getSprite(symbol);
                output.add(sprite);
                pool.remaining--;
                if (pool.remaining == 0)
                    generator.removePool(poolIndex);
            }
            return output;
        }

        private List<TrackedSequencePool> initGenerator() {
            return this.sequence.stream()
                    .map(TrackedSequencePool::new)
                    .collect(Collectors.toList());
        }

        private static final class TrackedSequencePool {

            private final SequencePool source;
            private int remaining;

            TrackedSequencePool(SequencePool source) {
                this.source = source;
                this.remaining = source.count();
            }
        }
    }

    private record SequencePool(String symbol, int count, int weight) {
        public static final Codec<SequencePool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("symbol").forGetter(SequencePool::symbol),
                Codec.INT.fieldOf("count").forGetter(SequencePool::count),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(SequencePool::weight)
        ).apply(instance, SequencePool::new));
        public static final StreamCodec<ByteBuf, SequencePool> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SequencePool::symbol,
                ByteBufCodecs.INT, SequencePool::count,
                ByteBufCodecs.INT, SequencePool::weight,
                SequencePool::new
        );
    }

    private static final class WeightedGenerator {

        private final List<Sequence.TrackedSequencePool> pools;
        private int totalWeight;

        public WeightedGenerator(List<Sequence.TrackedSequencePool> pools) {
            this.pools = pools;
            this.recalculateWeight();
        }

        public boolean canGenerate() {
            return !pools.isEmpty();
        }

        public int getPoolIndex(RandomSource random) {
            int value = random.nextInt(this.totalWeight);
            for (int i = this.pools.size() - 1; i >= 0; i--) {
                Sequence.TrackedSequencePool pool = this.pools.get(i);
                value -= pool.source.weight;
                if (value < 0) {
                    return i;
                }
            }
            throw new IllegalStateException("No pool found");
        }

        public Sequence.TrackedSequencePool getPool(int index) {
            return this.pools.get(index);
        }

        public void removePool(int index) {
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
