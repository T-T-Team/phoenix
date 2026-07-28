package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.sequence.EmptySequenceGenerator;
import dev.tnt.phoenix.data.sequence.SequenceGenerator;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

import java.util.*;
import java.util.function.Function;

public final class SlotMachineConfig {

    public static final Codec<SlotMachineConfig> CODEC = RecordCodecBuilder.<SlotMachineConfig>create(instance -> instance.group(
            SpinWheelEntry.CODEC.listOf().fieldOf("entries").forGetter(c -> new ArrayList<>(c.entries.values())),
            Codec.unboundedMap(GameType.CODEC, SequenceGenerator.CODEC.listOf()).fieldOf("sequence_generators").forGetter(c -> c.sequenceGenerators),
            WinConfigurationConfig.CODEC.fieldOf("win_configuration").forGetter(c -> c.winConfiguration)
    ).apply(instance, SlotMachineConfig::new)).validate(SlotMachineConfig::validateSymbolConsistency);
    public static final StreamCodec<ByteBuf, SlotMachineConfig> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final Map<String, SpinWheelEntry> entries;
    private final Map<GameType, List<SequenceGenerator>> sequenceGenerators;
    private final WinConfigurationConfig winConfiguration;

    private SlotMachineConfig(List<SpinWheelEntry> entries, Map<GameType, List<SequenceGenerator>> sequenceGenerators, WinConfigurationConfig winConfiguration) {
        this.entries = Util.make(new HashMap<>(), map -> entries.forEach(entry -> map.put(entry.id(), entry)));
        this.sequenceGenerators = sequenceGenerators;
        this.winConfiguration = winConfiguration;
    }

    public SpinWheelEntry getSymbolData(String symbol) {
        return this.entries.get(symbol);
    }

    public SequenceGenerator getSequenceGenerator(GameType type, int index) {
        List<SequenceGenerator> generators = this.sequenceGenerators.get(type);
        if (generators == null || generators.isEmpty())
            return EmptySequenceGenerator.INSTANCE;
        if (index < 0 || index >= generators.size())
            return EmptySequenceGenerator.INSTANCE;
        return generators.get(index);
    }

    public List<String> generateSequence(RandomSource random, GameType gameType, int sequenceIndex) {
        SequenceGenerator generator = this.getSequenceGenerator(gameType, sequenceIndex);
        List<String> output = new ArrayList<>();
        generator.generateSymbolSequence(random, output::add);
        return output;
    }

    public WinConfigurationConfig getWinningConfiguration() {
        return this.winConfiguration;
    }

    private static DataResult<SlotMachineConfig> validateSymbolConsistency(SlotMachineConfig config) {
        Set<String> uniqueSymbolSet = config.entries.keySet();
        // sequence generator check
        for (List<SequenceGenerator> sequenceGeneratorList : config.sequenceGenerators.values()) {
            for (SequenceGenerator generator : sequenceGeneratorList) {
                List<String> sequenceSymbols = generator.listAvailableSymbols();
                DataResult<SlotMachineConfig> checkResult = validateSequenceList(config, uniqueSymbolSet, sequenceSymbols, s -> "Found invalid symbol '" + s + "' in sequence generators");
                if (checkResult.isError()) {
                    return checkResult;
                }
            }
        }
        // wildcard check
        WinConfigurationConfig winCfg = config.winConfiguration;
        List<String> wildcards = winCfg.wildcards();
        DataResult<SlotMachineConfig> result = validateSequenceList(config, uniqueSymbolSet, wildcards, s -> "Found invalid symbol '" + s + "' in wildcard list");
        if (result.isError()) {
            return result;
        }
        // win combination check
        for (WinConfiguration winConfiguration : winCfg.gameConfiguration().values()) {
            for (WinCombination winCombination : winConfiguration.combinations()) {
                if (!uniqueSymbolSet.contains(winCombination.symbol())) {
                    return DataResult.error(() -> "Found invalid symbol '" + winCombination.symbol() + "' in win combinations");
                }
            }
        }
        return result;
    }

    private static DataResult<SlotMachineConfig> validateSequenceList(SlotMachineConfig config, Set<String> allowedSymbols, Collection<String> collection, Function<String, String> errorProvider) {
        for (String symbol : collection) {
            if (!allowedSymbols.contains(symbol)) {
                return DataResult.error(() -> errorProvider.apply(symbol));
            }
        }
        return DataResult.success(config);
    }
}
