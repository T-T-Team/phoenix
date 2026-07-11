package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.entity.player.Player;

import java.util.function.IntFunction;

public record C2S_SlotMachineRequest(BlockPos pos, RequestType requestType) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("slot_machine_request");
    public static final Type<C2S_SlotMachineRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, C2S_SlotMachineRequest> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2S_SlotMachineRequest::pos,
            RequestType.STREAM_CODEC, C2S_SlotMachineRequest::requestType,
            C2S_SlotMachineRequest::new
    );

    public void handle(Player player) {
        // TODO validations
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum RequestType {

        PLAY,
        RISK_HEARTS,
        RISK_CLUBS,
        HOLD_1,
        HOLD_2,
        HOLD_3,
        BET,
        ADVANCED,
        MULTIWIN,
        PAYOUT;

        public static final IntFunction<RequestType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, RequestType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

        public static RequestType holdActionFromIndex(int index) {
            return switch (index) {
                case 0 -> HOLD_1;
                case 1 -> HOLD_2;
                case 2 -> HOLD_3;
                default -> throw new IllegalStateException("Unexpected value: " + index);
            };
        }
    }
}
