package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.ActionType;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record C2S_SlotMachineRequest(BlockPos pos, ActionType actionType) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("slot_machine_request");
    public static final Type<C2S_SlotMachineRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, C2S_SlotMachineRequest> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2S_SlotMachineRequest::pos,
            ActionType.STREAM_CODEC, C2S_SlotMachineRequest::actionType,
            C2S_SlotMachineRequest::new
    );

    public void handle(Player player) {
        Level level = player.level();
        if (!level.isLoaded(this.pos))
            return;
        BlockEntity blockEntity = level.getBlockEntity(this.pos);
        if (!(blockEntity instanceof PhoenixSlotMachineBlockEntity slotMachineBlockEntity))
            return;
        String traceId = Phoenix.getTraceId(this.pos, player.getUUID());
        Phoenix.LOGGER.debug(Phoenix.NETWORK_MARKER, "[{}] Received slot machine request for action '{}'", traceId, this.actionType);
        slotMachineBlockEntity.performAction(player, this.actionType);
        slotMachineBlockEntity.updatePlayerView(player);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
