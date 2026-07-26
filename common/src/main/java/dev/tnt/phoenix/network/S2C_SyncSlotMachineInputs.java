package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.input.SlotMachineInput;
import dev.tnt.phoenix.data.input.SlotMachineInputManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record S2C_SyncSlotMachineInputs(List<SlotMachineInput> inputs) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("sync_slot_machine_inputs");
    public static final Type<S2C_SyncSlotMachineInputs> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SyncSlotMachineInputs> STREAM_CODEC = StreamCodec.composite(
            SlotMachineInput.STREAM_CODEC.apply(ByteBufCodecs.list()), S2C_SyncSlotMachineInputs::inputs,
            S2C_SyncSlotMachineInputs::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        SlotMachineInputManager inputManager = (SlotMachineInputManager) Phoenix.PLATFORM.getSlotMachineInputs();
        inputManager.receivePayload(this);
    }
}
