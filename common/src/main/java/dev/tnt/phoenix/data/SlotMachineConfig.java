package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.sequence.EmptySequenceGenerator;
import dev.tnt.phoenix.data.sequence.SequenceGenerator;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

import java.util.*;

public final class SlotMachineConfig {

    public static final Codec<SlotMachineConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpinWheelEntry.CODEC.listOf().fieldOf("entries").forGetter(c -> new ArrayList<>(c.entries.values())),
            Codec.unboundedMap(GameType.CODEC, SequenceGenerator.CODEC.listOf()).fieldOf("sequence_generators").forGetter(c -> c.sequenceGenerators),
            Codec.unboundedMap(GameType.CODEC, WinConfiguration.CODEC).fieldOf("win_configuration").forGetter(c -> c.winConfiguration)
    ).apply(instance, SlotMachineConfig::new));
    public static final StreamCodec<ByteBuf, SlotMachineConfig> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final Map<String, SpinWheelEntry> entries;
    private final Map<GameType, List<SequenceGenerator>> sequenceGenerators;
    private final Map<GameType, WinConfiguration> winConfiguration;

    private SlotMachineConfig(List<SpinWheelEntry> entries, Map<GameType, List<SequenceGenerator>> sequenceGenerators, Map<GameType, WinConfiguration> winConfiguration) {
        this.entries = Util.make(new HashMap<>(), map -> entries.forEach(entry -> map.put(entry.id(), entry)));
        this.sequenceGenerators = sequenceGenerators;
        this.winConfiguration = winConfiguration;
    }

    public Identifier getSprite(String symbol) {
        SpinWheelEntry entry = Objects.requireNonNull(this.entries.get(symbol), "No sprite defined for symbol: " + symbol);
        return entry.texturePath();
    }

    public SequenceGenerator getSequenceGenerator(GameType type, int index) {
        List<SequenceGenerator> generators = this.sequenceGenerators.get(type);
        if (generators == null || generators.isEmpty())
            return EmptySequenceGenerator.INSTANCE;
        if (index < 0 || index >= generators.size())
            return EmptySequenceGenerator.INSTANCE;
        return generators.get(index);
    }

    public List<Identifier> generateSequence(RandomSource random, GameType gameType, int sequenceIndex) {
        SequenceGenerator generator = this.getSequenceGenerator(gameType, sequenceIndex);
        List<Identifier> output = new ArrayList<>();
        generator.generateSymbolSequence(random, symbol -> {
            Identifier sprite = this.getSprite(symbol);
            output.add(sprite);
        });
        return output;
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
}
