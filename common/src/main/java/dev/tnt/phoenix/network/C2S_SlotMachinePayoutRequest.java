package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.PayoutRequestEntry;
import dev.tnt.phoenix.data.game.AccountBalance;
import dev.tnt.phoenix.data.game.AccountType;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
import dev.tnt.phoenix.data.payout.SlotMachinePayout;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutApi;
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

import static dev.tnt.phoenix.Phoenix.NETWORK_MARKER;

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
        String traceId = Phoenix.getTraceId(this.pos, player.getUUID());
        Phoenix.LOGGER.debug(NETWORK_MARKER, "[{}] Received slot machine payout request with {} unique payouts: {}", traceId, this.payouts.size(), this.payouts);
        if (!level.isLoaded(this.pos)) {
            Phoenix.LOGGER.warn(NETWORK_MARKER, "[{}] Discarding request as slot machine is not loaded at position {}", traceId, this.pos);
            return;
        }
        level.getBlockEntity(this.pos, Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get()).ifPresent(slotMachine -> {
            PlayerGameInstance instance = slotMachine.getPlayerData(player.getUUID());
            AccountBalance balance = instance.getAccountBalance();
            Phoenix.LOGGER.debug(NETWORK_MARKER, "[{}] Player linked correctly with slot machine and payout request. Available balance for payout: {}", traceId, balance.getMultiWinBalance());
            SlotMachinePayoutApi payoutApi = Phoenix.PLATFORM.getSlotMachinePayouts();
            for (PayoutRequestEntry request : this.payouts) {
                Optional<SlotMachinePayout> payoutOptional = payoutApi.findPayout(request);
                Phoenix.LOGGER.debug(NETWORK_MARKER, "[{}] Processing payout ID '{}': {}x {}", traceId, request.payoutId(), request.quantity(), payoutOptional);
                boolean success = payoutOptional.map(payout -> {
                    int price = payout.price();
                    for (int i = 0; i < request.quantity(); i++) {
                        if (!balance.hasBalanceInAccount(AccountType.MULTIWIN, price)) {
                            Phoenix.LOGGER.warn(NETWORK_MARKER, "[{}] Not enough balance in multiWin account to payout '{}', terminating payouts...", traceId, request.payoutId());
                            return false;
                        }
                        ItemStack itemStack = payout.assemble();
                        ItemStack insertItem = itemStack.copy();
                        if (!player.addItem(insertItem)) {
                            Phoenix.LOGGER.warn(NETWORK_MARKER, "[{}] Failed to insert payout '{}' result into inventory, terminating payouts...", traceId, request.payoutId());
                            if (itemStack.getCount() != insertItem.getCount()) {
                                Phoenix.LOGGER.warn(NETWORK_MARKER, "[{}] Payout was partially inserted to inventory, subtracting full price!", traceId);
                                balance.subtractBalance(AccountType.MULTIWIN, price);
                            }
                            return false;
                        }
                        balance.subtractBalance(AccountType.MULTIWIN, price);
                    }
                    return true;
                }).orElse(false);
                if (!success) {
                    Phoenix.LOGGER.warn(NETWORK_MARKER, "[{}] Failed to fully process payout, terminating...", traceId);
                    break;
                }
            }
            Phoenix.LOGGER.debug(NETWORK_MARKER, "[{}] Payout request processed successfully", traceId);
            slotMachine.markUpdated();
        });
    }
}
