package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.PhoenixSlotMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record S2C_OpenPhoenixMachineScreen(BlockPos pos) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("open_phoenix_machine_screen");
    public static final Type<S2C_OpenPhoenixMachineScreen> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, S2C_OpenPhoenixMachineScreen> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, S2C_OpenPhoenixMachineScreen::pos,
            S2C_OpenPhoenixMachineScreen::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        Minecraft instance = Minecraft.getInstance();
        instance.gui.setScreen(new PhoenixSlotMachineScreen(this.pos));
    }
}
