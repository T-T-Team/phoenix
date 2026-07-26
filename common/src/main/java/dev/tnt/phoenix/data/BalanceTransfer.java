package dev.tnt.phoenix.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.data.game.AccountType;
import dev.tnt.phoenix.data.game.Lock;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class BalanceTransfer {

    public static final Codec<BalanceTransfer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AccountType.CODEC.optionalFieldOf("source").forGetter(t -> t.source),
            AccountType.CODEC.fieldOf("target").forGetter(t -> t.target),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("amount", 0).forGetter(t -> t.amount),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("duration", 0).forGetter(t -> t.duration),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("transfer_amount", 0).forGetter(t -> t.transferAmount),
            InitiatorType.CODEC.optionalFieldOf("initiator_type", InitiatorType.SPIN).forGetter(t -> t.initiatorType)
    ).apply(instance, BalanceTransfer::new));
    private static final int TRANSFER_CYCLE_LENGTH = 5;

    private Optional<AccountType> source;
    private AccountType target;
    private int amount;
    private int duration;
    private int transferAmount;
    private InitiatorType initiatorType;

    private TransferHandler transferHandler;

    public BalanceTransfer(Optional<AccountType> source, AccountType target, int amount, int duration, int transferAmount, InitiatorType initiatorType) {
        this.source = source;
        this.target = target;
        this.amount = amount;
        this.duration = duration;
        this.transferAmount = transferAmount;
        this.initiatorType = initiatorType;
    }

    public static BalanceTransfer createInitial() {
        return new BalanceTransfer(
                Optional.empty(), AccountType.WIN, 0, 0, 0, InitiatorType.PENDING
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
            this.transferHandler.performTransferOperation(slotMachine, this.initiatorType, toTransfer, this.amount, this.source.orElse(null), this.target);
            if (this.getBalanceToTransfer() == 0) {
                this.onTransferFinished(slotMachine);
                return;
            }
        }
        if (--this.duration <= 0) {
            this.onTransferFinished(slotMachine);
        }
    }

    public void initiate(String sourceId, Optional<AccountType> sourceAccount, AccountType targetAccount, int amount, int totalDuration, InitiatorType initiatorType) {
        this.source = sourceAccount;
        this.target = targetAccount;
        this.amount = amount;
        this.duration = totalDuration;
        int transferCycles = totalDuration / TRANSFER_CYCLE_LENGTH;
        this.transferAmount = Math.max(amount / transferCycles, 1);
        this.initiatorType = initiatorType;
        Phoenix.LOGGER.debug("[{}] Initiating balance transfer of {}. Max duration is {} with transfer amount of {} per tick from account {} to {} initiated by {}", sourceId, amount, totalDuration, this.transferAmount, sourceAccount, targetAccount, initiatorType);
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
        if (this.amount > 0) {
            this.transferHandler.performTransferOperation(slotMachine, this.initiatorType, this.amount, 0, this.source.orElse(null), this.target);
        }
    }

    public void update(BalanceTransfer other) {
        this.source = other.source;
        this.target = other.target;
        this.amount = other.amount;
        this.duration = other.duration;
        this.transferAmount = other.transferAmount;
        this.initiatorType = other.initiatorType;
    }

    public enum InitiatorType implements StringRepresentable {

        PENDING("pending", Lock.EMPTY),
        SPIN("spin", Lock.SPIN),
        RISK("risk", Lock.RISK),
        RISK_TRANSFER("risk_transfer", Lock.TRANSFER);

        public static final Codec<InitiatorType> CODEC = StringRepresentable.fromEnum(InitiatorType::values);
        private final String serializedName;
        private final Lock lock;

        InitiatorType(String serializedName, Lock lock) {
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
        void performTransferOperation(PhoenixSlotMachineBlockEntity slotMachine, InitiatorType initiatorType, int transferBalance, int remainingBalance, @Nullable AccountType source, AccountType targetAccount);
    }
}
