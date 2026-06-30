package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.*;

public final class SlotMachineConfig {

    public static final Codec<SlotMachineConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpinWheelEntry.CODEC.listOf().fieldOf("entries").forGetter(c -> new ArrayList<>(c.entries.values())),
            Codec.unboundedMap(GameType.CODEC, Sequence.CODEC.listOf()).fieldOf("sequences").forGetter(c -> c.sequences),
            Codec.unboundedMap(GameType.CODEC, WinCombination.CODEC.listOf()).fieldOf("winning_combinations").forGetter(c -> c.winCombinations)
    ).apply(instance, SlotMachineConfig::new));
    public static final StreamCodec<FriendlyByteBuf, SlotMachineConfig> STREAM_CODEC = StreamCodec.composite(
            SpinWheelEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), c -> new ArrayList<>(c.entries.values()),
            ByteBufCodecs.map(HashMap::new, GameType.STREAM_CODEC, Sequence.STREAM_CODEC.apply(ByteBufCodecs.list())), c -> c.sequences,
            ByteBufCodecs.map(HashMap::new, GameType.STREAM_CODEC, WinCombination.STREAM_CODEC.apply(ByteBufCodecs.list())), cfg -> cfg.winCombinations,
            SlotMachineConfig::new
    );

    private final Map<String, SpinWheelEntry> entries;
    private final Map<GameType, List<Sequence>> sequences;
    private final Map<GameType, List<WinCombination>> winCombinations;

    private SlotMachineConfig(List<SpinWheelEntry> entries, Map<GameType, List<Sequence>> sequences, Map<GameType, List<WinCombination>> winCombinations) {
        this.entries = Util.make(new HashMap<>(), map -> entries.forEach(entry -> map.put(entry.id(), entry)));
        this.sequences = sequences;
        this.winCombinations = winCombinations;
    }

    public Identifier getSprite(String symbol) {
        SpinWheelEntry entry = Objects.requireNonNull(this.entries.get(symbol), "No sprite defined for symbol: " + symbol);
        return entry.texturePath();
    }

    public List<Identifier> getSprites() {
        return this.entries.values().stream()
                .filter(SpinWheelEntry::visible)
                .map(SpinWheelEntry::texturePath)
                .toList();
    }

    public List<WinCombination> getWinCombinations(GameType gameType, boolean renderable) {
        List<WinCombination> winCombinationList = this.winCombinations.get(gameType);
        if (winCombinationList == null)
            return Collections.emptyList();
        return winCombinationList.stream()
                .filter(combination -> combination.shouldRender(renderable))
                .sorted(Comparator.comparingInt(WinCombination::amount))
                .flatMap(WinCombination::spread)
                .toList();
    }

    private record Sequence(List<SequencePool> sequence) {
        public static final Codec<Sequence> CODEC = SequencePool.CODEC.listOf()
                .xmap(Sequence::new, Sequence::sequence);
        public static final StreamCodec<ByteBuf, Sequence> STREAM_CODEC = StreamCodec.composite(
                SequencePool.STREAM_CODEC.apply(ByteBufCodecs.list()), Sequence::sequence,
                Sequence::new
        );
    }

    private record SequencePool(String symbol, int count) {
        public static final Codec<SequencePool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("symbol").forGetter(SequencePool::symbol),
                Codec.INT.fieldOf("count").forGetter(SequencePool::count)
        ).apply(instance, SequencePool::new));
        public static final StreamCodec<ByteBuf, SequencePool> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SequencePool::symbol,
                ByteBufCodecs.INT, SequencePool::count,
                SequencePool::new
        );
    }
}
