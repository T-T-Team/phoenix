package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;

public final class SlotMachineConfig {

    public static final Codec<SlotMachineConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpinWheelEntry.CODEC.listOf().optionalFieldOf("spin_entries", Collections.emptyList()).forGetter(c -> new ArrayList<>(c.spinEntries.values()))
    ).apply(instance, SlotMachineConfig::new));
    public static final StreamCodec<FriendlyByteBuf, SlotMachineConfig> STREAM_CODEC = StreamCodec.composite(
            SpinWheelEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), c -> new ArrayList<>(c.spinEntries.values()),
            SlotMachineConfig::new
    );

    private final Map<String, SpinWheelEntry> spinEntries;

    private SlotMachineConfig(List<SpinWheelEntry> spinEntryList) {
        this.spinEntries = new LinkedHashMap<>();
        spinEntryList.forEach(entry -> this.spinEntries.put(entry.id(), entry));
    }
}
