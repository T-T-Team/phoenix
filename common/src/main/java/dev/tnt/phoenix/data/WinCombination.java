package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Set;

public record WinCombination(List<String> combination, int amount) {

    public static final Codec<WinCombination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf(1, 10).fieldOf("combination").forGetter(WinCombination::combination),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(WinCombination::amount)
    ).apply(instance, WinCombination::new));
    public static final StreamCodec<ByteBuf, WinCombination> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), WinCombination::combination,
            ByteBufCodecs.INT, WinCombination::amount,
            WinCombination::new
    );

    public boolean isWin(SlotMachineConfig config, List<String> value) {
        if (this.combination.size() != value.size())
            return false;
        Set<String> wildcards = config.getWildcards();
        for (int i = 0; i < value.size(); i++) {
            String entry = value.get(i);
            if (wildcards.contains(entry))
                continue;
            String combinationEntry = this.combination.get(i);
            if (combinationEntry.equals(entry))
                continue;
            return false;
        }
        return true;
    }
}
