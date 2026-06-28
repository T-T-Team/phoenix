package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.*;

public final class SlotMachineConfig {

    public static final Codec<SlotMachineConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpinWheelEntry.CODEC.listOf().optionalFieldOf("entries", Collections.emptyList()).forGetter(c -> new ArrayList<>(c.spinEntries.values())),
            Codec.STRING.listOf().optionalFieldOf("wildcards", Collections.emptyList()).forGetter(c -> new ArrayList<>(c.wildcards)),
            WinCombination.CODEC.listOf().optionalFieldOf("winning_combinations", Collections.emptyList()).forGetter(c -> c.winCombinations)
    ).apply(instance, SlotMachineConfig::new));
    public static final StreamCodec<FriendlyByteBuf, SlotMachineConfig> STREAM_CODEC = StreamCodec.composite(
            SpinWheelEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), cfg -> new ArrayList<>(cfg.spinEntries.values()),
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), cfg -> new ArrayList<>(cfg.wildcards),
            WinCombination.STREAM_CODEC.apply(ByteBufCodecs.list()), cfg -> cfg.winCombinations,
            SlotMachineConfig::new
    );

    private final Map<String, SpinWheelEntry> spinEntries;
    private final Set<String> wildcards;
    private final List<WinCombination> winCombinations;

    private SlotMachineConfig(List<SpinWheelEntry> spinEntryList, List<String> wildcards, List<WinCombination> winCombinations) {
        this.spinEntries = new LinkedHashMap<>();
        spinEntryList.forEach(entry -> this.spinEntries.put(entry.id(), entry));
        this.wildcards = new HashSet<>(wildcards);
        this.winCombinations = winCombinations;
    }

    public Set<String> getWildcards() {
        return wildcards;
    }

    public List<Identifier> getSprites() {
        return this.spinEntries.values().stream()
                .filter(SpinWheelEntry::isVisible)
                .map(SpinWheelEntry::sprite)
                .map(id -> id.withPath(path -> "textures/" + path + ".png"))
                .toList();
    }
}
