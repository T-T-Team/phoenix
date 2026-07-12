package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.PhoenixSlotMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record S2C_RefreshSlotMachine() implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("refresh_slot_machine");
    public static final Type<S2C_RefreshSlotMachine> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, S2C_RefreshSlotMachine> STREAM_CODEC = StreamCodec.unit(new S2C_RefreshSlotMachine());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        Minecraft client = Minecraft.getInstance();
        Screen screen = client.gui.screen();
        if (screen instanceof PhoenixSlotMachineScreen slotMachineScreen) {
            slotMachineScreen.forceReload();
        }
    }
}
