package dev.tnt.phoenix.block.entity;

import com.mojang.serialization.Codec;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.ItemValueDefinition;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.game.AccountBalance;
import dev.tnt.phoenix.data.game.BalanceType;
import dev.tnt.phoenix.data.game.Game;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
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
import net.minecraft.world.level.Level;
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
import java.util.function.IntConsumer;

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

    public void tick(Level level, BlockState blockState) {
        this.data.forEach(instance -> instance.tick(this));
    }

    public void performAction(Player player, ActionType actionType) {
        PlayerGameInstance instance = this.getPlayerData(player.getUUID());
        switch (actionType) {
            case PLAY -> this.play(instance, player);
            case BET -> this.bet(instance, player);
            case RISK_CLUBS -> this.risk(instance, player, false);
            case RISK_HEARTS -> this.risk(instance, player, true);
            case ADVANCED -> this.advancedPlay(instance, player);
            case MULTIWIN -> this.transferMultiWin(instance, player);
            case HOLD_1 -> this.hold(instance, 0);
            case HOLD_2 -> this.hold(instance, 1);
            case HOLD_3 -> this.hold(instance, 2);
            case PAYOUT -> this.payoutSelection(instance, player);
        }
        this.markUpdated();
    }

    public boolean insertItem(UUID owner, ItemInstance instance, boolean insertAll, ItemInsertionCallback insertionCallback) {
        int value = Phoenix.ITEM_VALUES.getItemValue(instance, insertAll);
        if (value > 0) {
            PlayerGameInstance holder = this.data.getData(owner);
            AccountBalance balance = holder.getAccountBalance();
            balance.addBalance(BalanceType.INPUT, value);
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
            PlayerGameInstance instance = PlayerGameInstance.createForPlayer(serverPlayer, config);
            this.data.data.put(serverPlayer.getUUID(), instance);
            this.markUpdated();
        }
    }

    // TODO validations everywhere
    private void play(PlayerGameInstance instance, Player player) {
        instance.startPlaying(this, player);
    }

    private void bet(PlayerGameInstance instance, Player player) {
        instance.toggleBetMultiplier();
    }

    private void risk(PlayerGameInstance instance, Player player, boolean riskHearts) {
        instance.startRisk(player, riskHearts);
    }

    private void advancedPlay(PlayerGameInstance instance, Player player) {
        Game game = instance.getGame();
        game.changeGameType();
    }

    private void transferMultiWin(PlayerGameInstance instance, Player player) {
        AccountBalance accountBalance = instance.getAccountBalance();
        int transferAmount = instance.getBetMultiplier().getValue(4);
        accountBalance.transferBalance(BalanceType.MULTIWIN, BalanceType.INPUT, transferAmount);
    }

    private void hold(PlayerGameInstance instance, int index) {
        instance.hold(index);
    }

    private void payoutSelection(PlayerGameInstance instance, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            int payoutBalance = instance.getAccountBalance().getBalance(BalanceType.MULTIWIN);
            List<ItemValueDefinition> availableItemValueDefinitions = Phoenix.ITEM_VALUES.getEntries();
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
