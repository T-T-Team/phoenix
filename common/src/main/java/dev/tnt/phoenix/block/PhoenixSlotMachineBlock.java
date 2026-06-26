package dev.tnt.phoenix.block;

import com.mojang.serialization.MapCodec;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.menu.PhoenixSlotMachineMenu;
import dev.tnt.phoenix.platform.init.PlatformMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class PhoenixSlotMachineBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<PhoenixSlotMachineBlock> CODEC = simpleCodec(PhoenixSlotMachineBlock::new);
    public static final Component NAME = Component.translatable("container.phoenix.phoenix_slot_machine");

    public PhoenixSlotMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            PhoenixSlotMachineBlockEntity blockEntity = (PhoenixSlotMachineBlockEntity) level.getBlockEntity(pos);
            Phoenix.PLATFORM.openMenu(serverPlayer, BlockPos.STREAM_CODEC, new PlatformMenuProvider<>() {
                @Override
                public Component title() {
                    return NAME;
                }
                @Override
                public BlockPos getMenuData(ServerPlayer player) {
                    return pos;
                }
                @Override
                public AbstractContainerMenu createMenu(int menuId, Inventory inventory, Player player) {
                    return new PhoenixSlotMachineMenu(menuId, inventory, blockEntity);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get().create(worldPosition, blockState);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
