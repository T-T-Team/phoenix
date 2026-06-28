package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.SlotMachineDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record S2C_SyncSlotMachineConfigs(List<SlotMachineDataManager.SlotMachineConfigWithId> definitions) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("sync_slot_machine_configs");
    public static final Type<S2C_SyncSlotMachineConfigs> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, S2C_SyncSlotMachineConfigs> STREAM_CODEC = StreamCodec.composite(
            SlotMachineDataManager.STREAM_CODEC, S2C_SyncSlotMachineConfigs::definitions,
            S2C_SyncSlotMachineConfigs::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        Phoenix.SLOT_MACHINES.receivePayload(this);
    }
}
