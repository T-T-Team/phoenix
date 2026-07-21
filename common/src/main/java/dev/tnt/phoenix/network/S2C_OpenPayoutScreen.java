package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.PayoutScreen;
import dev.tnt.phoenix.data.payout.SlotMachinePayout;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record S2C_OpenPayoutScreen(BlockPos pos, List<SlotMachinePayout> payouts, int balance) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("open_payout_screen");
    public static final Type<S2C_OpenPayoutScreen> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenPayoutScreen> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, S2C_OpenPayoutScreen::pos,
            SlotMachinePayout.STREAM_CODEC.apply(ByteBufCodecs.list()), S2C_OpenPayoutScreen::payouts,
            ByteBufCodecs.INT, S2C_OpenPayoutScreen::balance,
            S2C_OpenPayoutScreen::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(new PayoutScreen(minecraft.gui.screen(), this.pos, this.payouts, this.balance));
    }
}
