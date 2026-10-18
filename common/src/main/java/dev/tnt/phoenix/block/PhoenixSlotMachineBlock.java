package dev.tnt.phoenix.block;

import com.mojang.serialization.MapCodec;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.api.AccountType;
import dev.tnt.phoenix.network.S2C_OpenPhoenixMachineScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.UUID;

public class PhoenixSlotMachineBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<PhoenixSlotMachineBlock> CODEC = simpleCodec(PhoenixSlotMachineBlock::new);
    public static final Component NAME = Component.translatable("container.phoenix.phoenix_slot_machine");
    private static final VoxelShape HITBOX = Block.box(0.0, 0.0, 0.0, 16.0, 24.0, 16.0);

    public static final Component MESSAGE_ITEM_NOT_INSERTABLE = Component.translatable("message.phoenix.item_not_insertable").withStyle(ChatFormatting.RED);

    public PhoenixSlotMachineBlock(Properties properties) {
        super(properties.noOcclusion().strength(2.0F, 1.0F).lightLevel(state -> 8));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return HITBOX;
    }

    /*@Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return HITBOX;
    }*/

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            PhoenixSlotMachineBlockEntity blockEntity = (PhoenixSlotMachineBlockEntity) level.getBlockEntity(pos);
            blockEntity.onPlayerInteracted(serverPlayer);
            Phoenix.PLATFORM.sendPacket(serverPlayer, new S2C_OpenPhoenixMachineScreen(blockEntity.getBlockPos(), blockEntity.getUpdateTag(serverPlayer.registryAccess())));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (itemStack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            UUID uid = player.getUUID();
            PhoenixSlotMachineBlockEntity blockEntity = (PhoenixSlotMachineBlockEntity) level.getBlockEntity(pos);
            ServerPlayer serverPlayer = (ServerPlayer) player;
            blockEntity.onPlayerInteracted(serverPlayer);
            boolean inserted = blockEntity.insertItem(uid, itemStack, player.isCrouching(), (change, balance) -> {
                Component changeLabel = Component.literal(String.valueOf(change)).withStyle(ChatFormatting.YELLOW);
                Component balanceLabel = Component.literal(String.valueOf(balance.getBalance(AccountType.INPUT))).withStyle(ChatFormatting.YELLOW);
                player.displayClientMessage(Component.translatable("message.phoenix.item_inserted", changeLabel, balanceLabel).withStyle(ChatFormatting.GREEN), true);
            });
            if (inserted) {
                if (!player.isCreative()) {
                    itemStack.shrink(player.isCrouching() ? itemStack.getCount() : 1);
                }
            } else {
                player.displayClientMessage(MESSAGE_ITEM_NOT_INSERTABLE, true);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get().create(worldPosition, blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return type == Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get()
                ? (lvl, pos, state, slotMachine) -> ((PhoenixSlotMachineBlockEntity) slotMachine).tick()
                : null;
    }

    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        BlockPos relative = pos.relative(facing);
        Level level = context.getLevel();
        return level.getBlockState(relative).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(relative) ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
