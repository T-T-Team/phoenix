package dev.tnt.phoenix.data;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.network.S2C_SyncSlotMachineConfigs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public final class SlotMachineDataManager extends SimpleJsonResourceReloadListener<SlotMachineConfig> {

    public static final Identifier DATA_MANAGER_IDENTIFIER = Phoenix.identifier("slot_machine_manager");
    public static final StreamCodec<ByteBuf, List<SlotMachineConfigWithId>> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SlotMachineConfigWithId::id,
            SlotMachineConfig.STREAM_CODEC, SlotMachineConfigWithId::config,
            SlotMachineConfigWithId::new
    ).apply(ByteBufCodecs.list());
    private final Map<Identifier, SlotMachineConfigWithId> slotMachines = new HashMap<>();

    public SlotMachineDataManager() {
        super(SlotMachineConfig.CODEC, FileToIdConverter.json("slot_machine/config"));
    }

    public Optional<SlotMachineConfig> getSlotMachine(Identifier id) {
        Optional<SlotMachineConfigWithId> optional = Optional.ofNullable(this.slotMachines.get(id));
        return optional.map(SlotMachineConfigWithId::config);
    }

    public SlotMachineConfig getSlotMachineOrThrow(Identifier id) {
        return this.getSlotMachine(id)
                .orElseThrow(() -> new IllegalStateException("Slot machine with id " + id + " not found"));
    }

    public S2C_SyncSlotMachineConfigs getPayload() {
        return new S2C_SyncSlotMachineConfigs(new ArrayList<>(this.slotMachines.values()));
    }

    public void receivePayload(S2C_SyncSlotMachineConfigs payload) {
        this.slotMachines.clear();
        payload.definitions().forEach(config -> this.slotMachines.put(config.id(), config));
    }

    @Override
    protected void apply(Map<Identifier, SlotMachineConfig> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.slotMachines.clear();
        preparations.forEach((id, config) -> this.slotMachines.put(id, new SlotMachineConfigWithId(id, config)));
    }

    public record SlotMachineConfigWithId(Identifier id, SlotMachineConfig config) {
    }
}
