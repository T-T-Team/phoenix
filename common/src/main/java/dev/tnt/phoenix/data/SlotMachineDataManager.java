package dev.tnt.phoenix.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.network.S2C_SyncSlotMachineConfigs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

import static dev.tnt.phoenix.Phoenix.DATA_MARKER;

public final class SlotMachineDataManager extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation DATA_MANAGER_IDENTIFIER = Phoenix.identifier("slot_machine_manager");
    public static final StreamCodec<ByteBuf, List<SlotMachineConfigWithId>> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SlotMachineConfigWithId::id,
            SlotMachineConfig.STREAM_CODEC, SlotMachineConfigWithId::config,
            SlotMachineConfigWithId::new
    ).apply(ByteBufCodecs.list());
    private final Map<ResourceLocation, SlotMachineConfigWithId> slotMachines = new HashMap<>();

    public SlotMachineDataManager() {
        super(new Gson(), "slot_machine/config");
    }

    public Optional<SlotMachineConfig> getSlotMachine(ResourceLocation id) {
        Optional<SlotMachineConfigWithId> optional = Optional.ofNullable(this.slotMachines.get(id));
        return optional.map(SlotMachineConfigWithId::config);
    }

    public SlotMachineConfig getSlotMachineOrThrow(ResourceLocation id) {
        return this.getSlotMachine(id)
                .orElseThrow(() -> new IllegalStateException("Slot machine with id " + id + " not found"));
    }

    public S2C_SyncSlotMachineConfigs getPayload() {
        return new S2C_SyncSlotMachineConfigs(new ArrayList<>(this.slotMachines.values()));
    }

    public void receivePayload(S2C_SyncSlotMachineConfigs payload) {
        this.slotMachines.clear();
        payload.definitions().forEach(config -> this.slotMachines.put(config.id(), config));
        Phoenix.LOGGER.info(DATA_MARKER, "Received {} slot machine configs from server", this.slotMachines.size());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.slotMachines.clear();
        for (var entry : preparations.entrySet()) {
            ResourceLocation key = entry.getKey();
            JsonElement json = entry.getValue();
            DataResult<SlotMachineConfig> result = SlotMachineConfig.CODEC.parse(JsonOps.INSTANCE, json);
            result.ifError(error -> Phoenix.LOGGER.error(DATA_MARKER, "Failed to parse slot machine config '{}' due to error: {}", key, error))
                    .ifSuccess(value -> this.slotMachines.put(key, new SlotMachineConfigWithId(key, value)));
        }
        Phoenix.LOGGER.info(DATA_MARKER, "Loaded {} slot machine configs", this.slotMachines.size());
    }

    public record SlotMachineConfigWithId(ResourceLocation id, SlotMachineConfig config) {
    }
}
