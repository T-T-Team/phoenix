package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.data.game.BalanceType;
import dev.tnt.phoenix.data.game.Lock;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class MoneyTransfer {

    public static final Codec<MoneyTransfer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BalanceType.CODEC.optionalFieldOf("source").forGetter(t -> t.source),
            BalanceType.CODEC.fieldOf("target").forGetter(t -> t.target),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("amount", 0).forGetter(t -> t.amount),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("duration", 0).forGetter(t -> t.duration),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("transfer_amount", 0).forGetter(t -> t.transferAmount),
            TransferInitiatorType.CODEC.optionalFieldOf("initiator_type", TransferInitiatorType.SPIN).forGetter(t -> t.initiatorType)
    ).apply(instance, MoneyTransfer::new));
    private static final int TRANSFER_CYCLE_LENGTH = 5;

    private Optional<BalanceType> source;
    private BalanceType target;
    private int amount;
    private int duration;
    private int transferAmount;
    private TransferInitiatorType initiatorType;

    private TransferHandler transferHandler;

    public MoneyTransfer(Optional<BalanceType> source, BalanceType target, int amount, int duration, int transferAmount, TransferInitiatorType initiatorType) {
        this.source = source;
        this.target = target;
        this.amount = amount;
        this.duration = duration;
        this.transferAmount = transferAmount;
        this.initiatorType = initiatorType;
    }

    public static MoneyTransfer createInitial() {
        return new MoneyTransfer(
                Optional.empty(), BalanceType.WIN, 0, 0, 0, TransferInitiatorType.PENDING
        );
    }

    public void setTransferHandler(TransferHandler handler) {
        this.transferHandler = handler;
    }

    public void tick(PhoenixSlotMachineBlockEntity slotMachine) {
        if (!this.isActive() || slotMachine.getLevel().isClientSide())
            return;
        if (this.isTransferTick()) {
            int toTransfer = this.getBalanceToTransfer();
            this.amount -= toTransfer;
            this.transferHandler.performTransferOperation(slotMachine, this.initiatorType, toTransfer, this.amount, this.target);
            if (this.getBalanceToTransfer() == 0) {
                this.onTransferFinished(slotMachine);
                return;
            }
        }
        if (--this.duration <= 0) {
            this.onTransferFinished(slotMachine);
        }
    }

    public void initiate(Optional<BalanceType> sourceAccount, BalanceType targetAccount, int amount, int totalDuration, TransferInitiatorType initiatorType) {
        this.source = sourceAccount;
        this.target = targetAccount;
        this.amount = amount;
        this.duration = totalDuration;
        int transferCycles = totalDuration / TRANSFER_CYCLE_LENGTH;
        this.transferAmount = Math.max(amount / transferCycles, 1);
        this.initiatorType = initiatorType;
        Phoenix.LOGGER.debug("Initiating money transfer of {}. Max duration is {} with transfer amount of {} per tick from account {} to {} initiated by {}", amount, totalDuration, this.transferAmount, sourceAccount, targetAccount, initiatorType);
    }

    public boolean isActive() {
        return this.duration > 0;
    }

    private boolean isTransferTick() {
        return this.duration % TRANSFER_CYCLE_LENGTH == 0;
    }

    private int getBalanceToTransfer() {
        return Math.min(this.amount, this.transferAmount);
    }

    private void onTransferFinished(PhoenixSlotMachineBlockEntity slotMachine) {
        this.duration = 0;
        this.transferHandler.performTransferOperation(slotMachine, this.initiatorType, this.amount, 0, this.target);
    }

    public void update(MoneyTransfer other) {
        this.source = other.source;
        this.target = other.target;
        this.amount = other.amount;
        this.duration = other.duration;
        this.transferAmount = other.transferAmount;
        this.initiatorType = other.initiatorType;
    }

    public enum TransferInitiatorType implements StringRepresentable {

        PENDING("pending", Lock.EMPTY),
        SPIN("spin", Lock.SPIN),
        RISK("risk", Lock.RISK);

        public static final Codec<TransferInitiatorType> CODEC = StringRepresentable.fromEnum(TransferInitiatorType::values);
        private final String serializedName;
        private final Lock lock;

        TransferInitiatorType(String serializedName, Lock lock) {
            this.serializedName = serializedName;
            this.lock = lock;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public Lock getLock() {
            return this.lock;
        }
    }

    @FunctionalInterface
    public interface TransferHandler {
        void performTransferOperation(PhoenixSlotMachineBlockEntity slotMachine, TransferInitiatorType initiatorType, int transferBalance, int remainingBalance, BalanceType targetAccount);
    }
}
