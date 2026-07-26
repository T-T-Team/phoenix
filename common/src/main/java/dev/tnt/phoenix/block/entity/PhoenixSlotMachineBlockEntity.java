package dev.tnt.phoenix.block.entity;

import com.mojang.serialization.Codec;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.game.AccountBalance;
import dev.tnt.phoenix.data.game.AccountType;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
import dev.tnt.phoenix.data.input.SlotMachineInputApi;
import dev.tnt.phoenix.data.payout.SlotMachinePayout;
import dev.tnt.phoenix.data.payout.SlotMachinePayoutApi;
import dev.tnt.phoenix.network.S2C_OpenPayoutScreen;
import dev.tnt.phoenix.network.S2C_OpenPhoenixMachineScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class PhoenixSlotMachineBlockEntity extends BlockEntity {

    private final DataHolder data = DataHolder.create();

    public PhoenixSlotMachineBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get(), worldPosition, blockState);
    }

    public static SlotMachineConfig getConfig() {
        return Phoenix.SLOT_MACHINES.getSlotMachineOrThrow(Phoenix.SLOT_MACHINE_CONFIG_PHOENIX);
    }

    public PlayerGameInstance getPlayerData(UUID player) {
        return this.data.getData(player);
    }

    public void tick() {
        this.data.forEach(instance -> instance.tick(this));
    }

    public void performAction(Player player, ActionType actionType) {
        PlayerGameInstance instance = this.getPlayerData(player.getUUID());
        switch (actionType) {
            case PLAY -> instance.startPlaying(player);
            case BET -> instance.toggleBetMultiplier();
            case RISK_CLUBS -> instance.startRisk(player, false);
            case RISK_HEARTS -> instance.startRisk(player, true);
            case ADVANCED -> instance.swapGameType();
            case MULTIWIN -> instance.transferMultiWin();
            case HOLD_1 -> instance.hold(0);
            case HOLD_2 -> instance.hold(1);
            case HOLD_3 -> instance.hold(2);
            case PAYOUT -> this.payoutSelection(instance, player);
        }
        this.markUpdated();
    }

    public boolean insertItem(UUID owner, ItemInstance instance, boolean insertAll, ItemInsertionCallback insertionCallback) {
        SlotMachineInputApi inputApi = Phoenix.PLATFORM.getSlotMachineInputs();
        int value = inputApi.getItemValue(instance, insertAll);
        if (value > 0) {
            PlayerGameInstance holder = this.data.getData(owner);
            AccountBalance balance = holder.getAccountBalance();
            balance.addBalance(AccountType.INPUT, value);
            insertionCallback.onInsertion(value, balance);
            this.markUpdated();
            return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.store("data", DataHolder.CODEC, this.data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        DataHolder holder = input.read("data", DataHolder.CODEC)
                .orElseGet(DataHolder::create);
        this.data.update(holder);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public void markUpdated() {
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    public void updatePlayerView(Player player) {
        if (player.level().isClientSide())
            return;
        S2C_OpenPhoenixMachineScreen packet = new S2C_OpenPhoenixMachineScreen(this.getBlockPos(), this.getUpdateTag(player.registryAccess()), true);
        Phoenix.PLATFORM.sendPacket((ServerPlayer) player, packet);
    }

    public void onPlayerInteracted(ServerPlayer serverPlayer) {
        if (!this.data.data.containsKey(serverPlayer.getUUID())) {
            SlotMachineConfig config = Phoenix.SLOT_MACHINES.getSlotMachineOrThrow(Phoenix.SLOT_MACHINE_CONFIG_PHOENIX);
            PlayerGameInstance instance = PlayerGameInstance.createForPlayer(serverPlayer, this.getBlockPos(), config);
            this.data.data.put(serverPlayer.getUUID(), instance);
            this.markUpdated();
        }
    }

    private void payoutSelection(PlayerGameInstance instance, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            int payoutBalance = instance.getAccountBalance().getBalance(AccountType.MULTIWIN);
            SlotMachinePayoutApi payoutApi = Phoenix.PLATFORM.getSlotMachinePayouts();
            List<SlotMachinePayout> availableItemValueDefinitions = payoutApi.listAvailablePayouts();
            Phoenix.PLATFORM.sendPacket(serverPlayer, new S2C_OpenPayoutScreen(this.getBlockPos(), availableItemValueDefinitions, payoutBalance));
        }
    }

    private record DataHolder(Map<UUID, PlayerGameInstance> data) {

        public static final Codec<DataHolder> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerGameInstance.CODEC)
                .xmap(DataHolder::new, holder -> holder.data);

        private DataHolder(Map<UUID, PlayerGameInstance> data) {
            this.data = new HashMap<>(data);
        }

        public static DataHolder create() {
            return new DataHolder(new HashMap<>());
        }

        public PlayerGameInstance getData(UUID player) {
            return this.data.get(player);
        }

        public void update(DataHolder source) {
            for (var entry : source.data.entrySet()) {
                this.data.merge(entry.getKey(), entry.getValue(), PlayerGameInstance::update);
            }
        }

        public void forEach(Consumer<PlayerGameInstance> consumer) {
            this.data.values().forEach(consumer);
        }
    }

    @FunctionalInterface
    public interface ItemInsertionCallback {
        void onInsertion(int balanceChange, AccountBalance balance);
    }
}
