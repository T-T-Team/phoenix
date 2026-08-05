package dev.tnt.phoenix.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.*;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class PlayerGameInstance {

    public static final Marker MARKER = MarkerManager.getMarker("GameManager");
    public static final Codec<PlayerGameInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(t -> t.owner),
            Codec.STRING.optionalFieldOf("trace_id", "<no trace id>").forGetter(t -> t.traceId),
            AccountBalanceComponent.CODEC.fieldOf("account_balance").forGetter(PlayerGameInstance::getAccountBalance),
            AccountBalanceTransactionComponent.CODEC.optionalFieldOf("balance_transfer", AccountBalanceTransactionComponent.createInitial()).forGetter(t -> t.transactions),
            SpinGameComponent.CODEC.fieldOf("spin_game").forGetter(t -> t.spinGame),
            RiskGameComponent.CODEC.fieldOf("risk_game").forGetter(t -> t.riskGame),
            Lock.CODEC.optionalFieldOf("lock", Lock.EMPTY).forGetter(t -> t.lock)
    ).apply(instance, PlayerGameInstance::new));

    private final UUID owner;
    private final String traceId;
    private final AccountBalanceComponent accountBalance;
    private final AccountBalanceTransactionComponent transactions;
    private final SpinGameComponent spinGame;
    private final RiskGameComponent riskGame;
    private Lock lock;

    private boolean needsSynchronization;

    private PlayerGameInstance(UUID owner, String traceId, AccountBalanceComponent accountBalance, AccountBalanceTransactionComponent transactions, SpinGameComponent spinGame, RiskGameComponent riskGame, Lock lock) {
        this.owner = owner;
        this.traceId = traceId;
        this.accountBalance = accountBalance;
        this.transactions = transactions;
        this.spinGame = spinGame;
        this.riskGame = riskGame;
        this.lock = lock;

        InstanceAccess access = new InstanceAccess(this);
        this.accountBalance.setInstanceAccess(access);
        this.transactions.setInstanceAccess(access);
        this.spinGame.setInstanceAccess(access);
        this.riskGame.setInstanceAccess(access);

        this.transactions.setTransferHandler(this::handleBalanceTransferTick);
    }

    public static PlayerGameInstance createForPlayer(ServerPlayer player, BlockPos position) {
        RandomSource random = player.getRandom();
        UUID playerId = player.getUUID();
        String traceId = Phoenix.getTraceId(position, playerId);

        SpinGameComponent spinGameComponent = SpinGameComponent.initComponent(random);
        RiskGameComponent riskGameComponent = RiskGameComponent.initComponent();

        return new PlayerGameInstance(
                playerId,
                traceId,
                AccountBalanceComponent.createDefault(),
                AccountBalanceTransactionComponent.createInitial(),
                spinGameComponent,
                riskGameComponent,
                Lock.EMPTY
        );
    }

    public void tick(Level level, BlockPos pos) {
        this.transactions.tick(level, pos);
        this.spinGame.tick(level, pos);
        this.riskGame.tick(level, pos);

        if (this.needsSynchronization) {
            this.updateSlotMachineAndView(level, pos);
        }
    }

    public SpinGameComponent getSpinGame() {
        return this.spinGame;
    }

    public RiskGameComponent getRiskGame() {
        return this.riskGame;
    }

    public boolean canWithdrawMultiWin() {
        if (this.isLocked())
            return false;
        Bet bet = this.spinGame.bet();
        int amount = bet.getValue(Phoenix.CONFIG.multiWinSpinPriceMultiplier);
        return this.accountBalance.hasBalanceInAccount(AccountType.MULTIWIN, amount);
    }

    public void transferMultiWin() {
        Bet bet = this.spinGame.bet();
        int requestAmount = bet.getValue(Phoenix.CONFIG.multiWinSpinPriceMultiplier);
        Phoenix.LOGGER.debug(MARKER, "[{}] Attempting to withdraw {} from multiWin account", this.traceId, requestAmount);
        if (!this.canWithdrawMultiWin()) {
            int balance = this.accountBalance.getBalance(AccountType.MULTIWIN);
            Phoenix.LOGGER.error(MARKER, "[{}] Failed to withdraw {} from multiWin account, not enough balance. Available balance: {}", this.traceId, requestAmount, balance);
            return;
        }
        this.accountBalance.transferBalance(AccountType.MULTIWIN, AccountType.INPUT, requestAmount);
    }

    public AccountBalanceComponent getAccountBalance() {
        return accountBalance;
    }

    public void insertBalance(ItemInstance instance, int value, PhoenixSlotMachineBlockEntity.ItemInsertionCallback insertionCallback) {
        Phoenix.LOGGER.debug(MARKER, "[{}] Inserting item {} with value of {}", this.traceId, instance, value);
        this.accountBalance.addBalance(AccountType.INPUT, value);
        insertionCallback.onInsertion(value, this.accountBalance);
    }

    public void lock(Lock lock) {
        this.lock = lock;
        Phoenix.LOGGER.debug(MARKER, "[{}] Locking slot machine with lock {}", this.traceId, lock);
        this.markForUpdate();
    }

    public void unlock(@Nullable Lock lock) {
        Phoenix.LOGGER.debug(MARKER, "[{}] Unlocking slot machine lock {}", this.traceId, lock);
        if (!this.lock.locked()) {
            Phoenix.LOGGER.warn(MARKER, "[{}] Failed to unlock: not locked", this.traceId);
            return;
        }
        if (lock != null && !this.lock.equals(lock)) {
            Phoenix.LOGGER.warn(MARKER, "[{}] Failed to unlock: expected {}, got {}", this.traceId, this.lock, lock);
            return;
        }
        this.lock = Lock.EMPTY;
        this.markForUpdate();
    }

    public boolean isLocked() {
        return this.lock.locked();
    }

    public LockReason getLockReason() {
        return this.lock.reason();
    }

    public PlayerGameInstance update(PlayerGameInstance holder) {
        this.accountBalance.updateFrom(holder.accountBalance);
        this.transactions.updateFrom(holder.transactions);
        this.spinGame.updateFrom(holder.spinGame);
        this.riskGame.updateFrom(holder.riskGame);
        this.lock = holder.lock;
        return this;
    }

    private void handleBalanceTransferTick(TransactionSource initiatorType, int amount, int remaining, @Nullable AccountType source, AccountType targetAccount) {
        this.accountBalance.addBalance(targetAccount, amount);
        if (source != null) {
            this.accountBalance.subtractBalance(source, amount);
        }
        if (remaining <= 0) {
            Phoenix.LOGGER.debug(AccountBalanceTransactionComponent.MARKER, "[{}] Balance transfer finished from source {}", this.traceId, initiatorType);
            if (this.getLockReason() != LockReason.SPIN) {
                this.unlock(initiatorType.getLock());
            }
            if (initiatorType == TransactionSource.SPIN) {
                this.spinGame.onTransactionCompleted();
            }
        }
        this.markForUpdate();
    }

    private void markForUpdate() {
        this.needsSynchronization = true;
    }

    private void updateSlotMachineAndView(Level level, BlockPos pos) {
        level.getBlockEntity(pos, Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get()).ifPresent(slotMachine -> {
            slotMachine.markUpdated();
            Player player = level.getPlayerByUUID(this.owner);
            if (player != null) {
                slotMachine.updatePlayerView(player);
            }
        });
        this.needsSynchronization = false;
    }

    private record InstanceAccess(PlayerGameInstance delegate) implements GameInstanceAccess {

        @Override
        public String traceId() {
            return this.delegate.traceId;
        }

        @Override
        public void setChanged() {
            this.delegate.markForUpdate();
        }

        @Override
        public void lock(Lock lock) {
            this.delegate.lock(lock);
        }

        @Override
        public void unlock(@Nullable Lock lock) {
            this.delegate.unlock(lock);
        }

        @Override
        public boolean isLocked() {
            return this.delegate.isLocked();
        }

        @Override
        public boolean isLockedWithReason(LockReason reason, LockReason... other) {
            return this.isLocked() && this.delegate.lock.reason().is(reason, other);
        }

        @Override
        public AccountBalance account() {
            return this.delegate.accountBalance;
        }

        @Override
        public AccountBalanceTransaction transactions() {
            return this.delegate.transactions;
        }

        @Override
        public SpinGame spin() {
            return this.delegate.spinGame;
        }

        @Override
        public RiskGame risk() {
            return this.delegate.riskGame;
        }
    }
}
