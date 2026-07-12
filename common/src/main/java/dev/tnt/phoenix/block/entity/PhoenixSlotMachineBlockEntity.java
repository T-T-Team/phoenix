package dev.tnt.phoenix.block.entity;

import com.mojang.serialization.Codec;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.game.AccountBalance;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

    public void startGame(UUID player) {
        PlayerGameInstance holder = this.getPlayerData(player);
        if (!holder.canPlay())
            return;
        holder.play();
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
            return this.data.computeIfAbsent(player, _ -> PlayerGameInstance.createDefault());
        }

        public void update(DataHolder source) {
            for (var entry : source.data.entrySet()) {
                this.data.merge(entry.getKey(), entry.getValue(), PlayerGameInstance::update);
            }
        }
    }
}
