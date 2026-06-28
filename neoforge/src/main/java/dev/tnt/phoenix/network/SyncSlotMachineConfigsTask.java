package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

public record SyncSlotMachineConfigsTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {

    public static final Type TYPE = new Type(S2C_SyncSlotMachineConfigs.ID);

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(Phoenix.SLOT_MACHINES.getPayload());
        this.listener.finishCurrentTask(TYPE);
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
