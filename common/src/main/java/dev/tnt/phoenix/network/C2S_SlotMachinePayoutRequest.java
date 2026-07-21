package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.PayoutRequestEntry;
import dev.tnt.phoenix.data.game.AccountBalance;
import dev.tnt.phoenix.data.game.BalanceType;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
import dev.tnt.phoenix.data.payout.SlotMachinePayout;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public record C2S_SlotMachinePayoutRequest(BlockPos pos, List<PayoutRequestEntry> payouts) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("slot_machine_payout_request");
    public static final Type<C2S_SlotMachinePayoutRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_SlotMachinePayoutRequest> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2S_SlotMachinePayoutRequest::pos,
            PayoutRequestEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), C2S_SlotMachinePayoutRequest::payouts,
            C2S_SlotMachinePayoutRequest::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(Player player) {
        Level level = player.level();
        if (!level.isLoaded(this.pos))
            return;
        level.getBlockEntity(this.pos, Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get()).ifPresent(slotMachine -> {
            PlayerGameInstance instance = slotMachine.getPlayerData(player.getUUID());
            AccountBalance balance = instance.getAccountBalance();
            SlotMachinePayoutManager manager = Phoenix.PAYOUT_MANAGER;
            for (PayoutRequestEntry request : this.payouts) {
                Optional<SlotMachinePayout> payoutOptional = manager.getPayout(request.payoutId());
                payoutOptional.ifPresent(payout -> {
                    int price = payout.price();
                    for (int i = 0; i < request.quantity(); i++) {
                        if (!balance.hasSufficientBalance(BalanceType.MULTIWIN, price)) {
                            break;
                        }
                        ItemStack itemStack = payout.assemble();
                        if (!player.addItem(itemStack)) {
                            break;
                        }
                        balance.subtractBalance(BalanceType.MULTIWIN, price);
                    }
                });
            }
            slotMachine.markUpdated();
        });
    }
}
