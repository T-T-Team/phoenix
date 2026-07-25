package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.PhoenixClient;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record S2C_OpenPhoenixMachineScreen(BlockPos pos, CompoundTag tag, boolean refreshOnly) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("open_phoenix_machine_screen");
    public static final Type<S2C_OpenPhoenixMachineScreen> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, S2C_OpenPhoenixMachineScreen> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, S2C_OpenPhoenixMachineScreen::pos,
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, S2C_OpenPhoenixMachineScreen::tag,
            ByteBufCodecs.BOOL, S2C_OpenPhoenixMachineScreen::refreshOnly,
            S2C_OpenPhoenixMachineScreen::new
    );

    public S2C_OpenPhoenixMachineScreen(BlockPos pos, CompoundTag tag) {
        this(pos, tag, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        PhoenixClient.handlePhoenixScreenOpenRequest(this);
    }
}
