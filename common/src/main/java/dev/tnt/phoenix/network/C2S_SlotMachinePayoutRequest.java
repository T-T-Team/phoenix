package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.ItemValueDefinitionManager;
import dev.tnt.phoenix.data.ItemValueHolder;
import dev.tnt.phoenix.data.game.AccountBalance;
import dev.tnt.phoenix.data.game.BalanceType;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
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

public record C2S_SlotMachinePayoutRequest(BlockPos pos, List<ItemValueHolder> payouts) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("slot_machine_payout_request");
    public static final Type<C2S_SlotMachinePayoutRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_SlotMachinePayoutRequest> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2S_SlotMachinePayoutRequest::pos,
            ItemValueHolder.STREAM_CODEC.apply(ByteBufCodecs.list()), C2S_SlotMachinePayoutRequest::payouts,
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
            int requestPrice = calculateTotalPrice();
            if (requestPrice <= balance.getBalance(BalanceType.MULTIWIN)) {
                ItemValueDefinitionManager manager = Phoenix.ITEM_VALUES;
                for (ItemValueHolder payout : this.payouts) {
                    int price = manager.getItemValue(payout.item());
                    if (price <= 0) {
                        Phoenix.LOGGER.warn("Invalid item value for payout: {}", payout);
                        continue;
                    }
                    int totalPrice = price * payout.value();
                    int remainingQt = payout.value();
                    int stack = payout.item().getDefaultMaxStackSize();
                    while (remainingQt > 0) {
                        int payoutStack = Math.min(stack, remainingQt);
                        remainingQt -= payoutStack;
                        ItemStack itemStack = new ItemStack(payout.item(), payoutStack);
                        if (!player.addItem(itemStack)) {
                            int remainingValue = (remainingQt + itemStack.getCount()) * price;
                            totalPrice -= remainingValue;
                            break;
                        }
                    }
                    balance.subtractBalance(BalanceType.MULTIWIN, totalPrice);
                }
                slotMachine.markUpdated();
            }
        });
    }

    private int calculateTotalPrice() {
        ItemValueDefinitionManager manager = Phoenix.ITEM_VALUES;
        return this.payouts.stream().mapToInt(holder -> {
            int price = manager.getItemValue(holder.item());
            return price * holder.value();
        }).sum();
    }
}
