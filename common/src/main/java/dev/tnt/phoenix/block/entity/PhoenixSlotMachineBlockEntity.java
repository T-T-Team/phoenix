package dev.tnt.phoenix.block.entity;

import com.mojang.serialization.Codec;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.GameType;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.game.AccountBalance;
import dev.tnt.phoenix.data.game.Game;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
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
import java.util.Map;
import java.util.UUID;

public final class PhoenixSlotMachineBlockEntity extends BlockEntity {

    private DataHolder data = DataHolder.create();

    public PhoenixSlotMachineBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get(), worldPosition, blockState);
    }

    public PlayerGameInstance getPlayerData(UUID player) {
        return this.data.getData(player);
    }

    public void performAction(Player player, ActionType actionType) {
        PlayerGameInstance instance = this.getPlayerData(player.getUUID());
        switch (actionType) {
            case PLAY -> this.play(instance, player);
            case BET -> this.bet(instance, player);
        }
        this.markUpdated();
    }

    public boolean insertItem(UUID owner, ItemInstance instance, boolean insertAll) {
        int value = Phoenix.ITEM_VALUES.getItemValue(instance, insertAll);
        if (value > 0) {
            PlayerGameInstance holder = this.data.getData(owner);
            AccountBalance balance = holder.getAccountBalance();
            balance.addBalance(value);
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

    public void onPlayerInteracted(ServerPlayer serverPlayer) {
        if (!this.data.data.containsKey(serverPlayer.getUUID())) {
            SlotMachineConfig config = Phoenix.SLOT_MACHINES.getSlotMachineOrThrow(Phoenix.SLOT_MACHINE_CONFIG_PHOENIX);
            PlayerGameInstance instance = PlayerGameInstance.createForPlayer(serverPlayer, config);
            this.data.data.put(serverPlayer.getUUID(), instance);
            this.markUpdated();
        }
    }

    private void play(PlayerGameInstance instance, Player player) {
        Game game = instance.getGame();
        AccountBalance balance = instance.getAccountBalance();
        int balanceCost = instance.getCost(GameType.LOW);
        balance.subtractBalance(balanceCost);
        if (game.getSelectedGameType() == GameType.HIGH) {
            int balanceCostMultiWin = instance.getCost(GameType.HIGH);
            balance.subtractMultiWinBalance(balanceCostMultiWin);
        }
    }

    private void bet(PlayerGameInstance instance, Player player) {
        instance.toggleDoubleWins();
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
    }
}
