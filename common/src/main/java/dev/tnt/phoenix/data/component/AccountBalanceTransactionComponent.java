package dev.tnt.phoenix.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class AccountBalanceTransactionComponent extends PhoenixComponent implements AccountBalanceTransaction {

    public static final Marker MARKER = MarkerManager.getMarker("Transaction");
    public static final int TRANSFER_CYCLE_LENGTH = 2;

    public static final Codec<AccountBalanceTransactionComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AccountType.CODEC.optionalFieldOf("source").forGetter(t -> t.source),
            AccountType.CODEC.fieldOf("target").forGetter(t -> t.target),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("amount", 0).forGetter(t -> t.amount),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("duration", 0).forGetter(t -> t.duration),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("transfer_amount", 0).forGetter(t -> t.transferAmount),
            TransactionSource.CODEC.fieldOf("transaction_source").forGetter(t -> t.transactionSource)
    ).apply(instance, AccountBalanceTransactionComponent::new));

    private Optional<AccountType> source;
    private AccountType target;
    private int amount;
    private int duration;
    private int transferAmount;
    private TransactionSource transactionSource;

    private TransferHandler transferHandler;

    public AccountBalanceTransactionComponent(Optional<AccountType> source, AccountType target, int amount, int duration, int transferAmount, TransactionSource transactionSource) {
        this.source = source;
        this.target = target;
        this.amount = amount;
        this.duration = duration;
        this.transferAmount = transferAmount;
        this.transactionSource = transactionSource;
    }

    public static AccountBalanceTransactionComponent createInitial() {
        return new AccountBalanceTransactionComponent(
                Optional.empty(), AccountType.WIN, 0, 0, 0, TransactionSource.PENDING
        );
    }

    public void setTransferHandler(TransferHandler handler) {
        this.transferHandler = handler;
    }

    public void tick(Level level, BlockPos pos) {
        if (!this.isActive() || level.isClientSide())
            return;
        if (this.isTransferTick()) {
            int toTransfer = this.getBalanceToTransfer();
            this.amount -= toTransfer;
            this.transferHandler.performTransferOperation(this.transactionSource, toTransfer, this.amount, this.source.orElse(null), this.target);
            if (this.getBalanceToTransfer() == 0) {
                this.onTransferFinished();
                return;
            }
        }
        if (--this.duration <= 0) {
            this.onTransferFinished();
        }
    }

    @Override
    public void initiate(TransactionSource source, Optional<AccountType> sourceAccount, AccountType destAccount, int transactionVolume) {
        int transferDuration = this.getBalanceTransferDuration(source);
        this.source = sourceAccount;
        this.target = destAccount;
        this.amount = transactionVolume;
        this.duration = transferDuration;
        int transferCycles = transferDuration / TRANSFER_CYCLE_LENGTH;
        this.transferAmount = Math.max(Math.round((float) transactionVolume / transferCycles), 1);
        this.transactionSource = source;
        Phoenix.LOGGER.debug(MARKER, "[{}] Initiating transaction of {} volume. Max duration is {} with transfer amount of {} per tick from account {} to {} initiated by {}", this.instanceAccess.traceId(), this.amount, transferDuration, this.transferAmount, this.source, this.target, this.source);
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

    private void onTransferFinished() {
        this.duration = 0;
        if (this.amount > 0) {
            this.transferHandler.performTransferOperation(this.transactionSource, this.amount, 0, this.source.orElse(null), this.target);
        }
    }

    public void updateFrom(AccountBalanceTransactionComponent other) {
        this.source = other.source;
        this.target = other.target;
        this.amount = other.amount;
        this.duration = other.duration;
        this.transferAmount = other.transferAmount;
        this.transactionSource = other.transactionSource;
    }

    private int getBalanceTransferDuration(TransactionSource transactionSource) {
        SpinGame spinGame = this.instanceAccess.spin();
        Bet bet = spinGame.bet();
        return switch (transactionSource) {
            case SPIN -> bet.getBalanceTransferDuration();
            case RISK_TRANSFER -> 50;
            case PENDING -> throw new IllegalArgumentException("Invalid transaction source for balance transfer: " + transactionSource);
        };
    }

    @FunctionalInterface
    public interface TransferHandler {
        void performTransferOperation(TransactionSource transactionSource, int transferBalance, int remainingBalance, @Nullable AccountType source, AccountType targetAccount);
    }
}
