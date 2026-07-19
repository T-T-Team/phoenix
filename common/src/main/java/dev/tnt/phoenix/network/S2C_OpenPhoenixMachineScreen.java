package dev.tnt.phoenix.network;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.PhoenixSlotMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

public record S2C_OpenPhoenixMachineScreen(BlockPos pos, CompoundTag tag, boolean refreshOnly) implements CustomPacketPayload {

    public static final Identifier ID = Phoenix.identifier("open_phoenix_machine_screen");
    public static final Type<S2C_OpenPhoenixMachineScreen> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, S2C_OpenPhoenixMachineScreen> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, S2C_OpenPhoenixMachineScreen::pos,
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, S2C_OpenPhoenixMachineScreen::tag,
            ByteBufCodecs.BOOL, S2C_OpenPhoenixMachineScreen::refreshOnly,
            S2C_OpenPhoenixMachineScreen::new
    );

    public S2C_OpenPhoenixMachineScreen(BlockPos pos, CompoundTag tag) {
        this(pos, tag, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        Minecraft instance = Minecraft.getInstance();
        instance.level.getBlockEntity(this.pos, Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get()).ifPresent(slotMachine -> {
            ProblemReporter problemReporter = new ProblemReporter.ScopedCollector(slotMachine.problemPath(), Phoenix.LOGGER_SLF4J);
            ValueInput valueInput = TagValueInput.create(problemReporter, instance.level.registryAccess(), this.tag);
            slotMachine.loadWithComponents(valueInput);
            if (this.refreshOnly && !(instance.gui.screen() instanceof PhoenixSlotMachineScreen)) {
                return;
            }
            instance.gui.setScreen(new PhoenixSlotMachineScreen(this.pos));
        });
    }
}
