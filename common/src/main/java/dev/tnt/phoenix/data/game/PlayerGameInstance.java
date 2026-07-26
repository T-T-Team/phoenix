package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.config.PhoenixConfig;
import dev.tnt.phoenix.data.*;
import dev.tnt.phoenix.util.EnumHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerGameInstance {

    public static final Marker MARKER = MarkerManager.getMarker("Game");
    public static final Codec<PlayerGameInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(t -> t.owner),
            Codec.STRING.optionalFieldOf("trace_id", "<no trace id>").forGetter(t -> t.traceId),
            AccountBalance.CODEC.fieldOf("account_balance").forGetter(PlayerGameInstance::getAccountBalance),
            Game.CODEC.fieldOf("active_spin").forGetter(t -> t.game),
            SpinWheel.CODEC.listOf().fieldOf("spin_wheels").forGetter(t -> t.spinWheels),
            GameWinInfo.CODEC.optionalFieldOf("game_win_info", GameWinInfo.create()).forGetter(t -> t.winInfo),
            BalanceTransfer.CODEC.optionalFieldOf("balance_transfer", BalanceTransfer.createInitial()).forGetter(t -> t.balanceTransfer),
            BetMultiplier.CODEC.optionalFieldOf("bet_multiplier", BetMultiplier.X1).forGetter(t -> t.betMultiplier),
            Codec.INT.optionalFieldOf("spins", 0).forGetter(t -> t.pendingSpins),
            Lock.CODEC.optionalFieldOf("lock", Lock.EMPTY).forGetter(t -> t.lock)
    ).apply(instance, PlayerGameInstance::new));

    private final UUID owner;
    private final String traceId;
    private final AccountBalance accountBalance;
    private final Game game;
    private final List<SpinWheel> spinWheels;
    private final GameWinInfo winInfo;
    private final BalanceTransfer balanceTransfer;
    private BetMultiplier betMultiplier;
    private int pendingSpins;
    private Lock lock;

    private boolean needsSynchronization;

    private PlayerGameInstance(UUID owner, String traceId, AccountBalance accountBalance, Game game, List<SpinWheel> spinWheels, GameWinInfo winInfo, BalanceTransfer balanceTransfer, BetMultiplier betMultiplier, int pendingSpins, Lock lock) {
        this.owner = owner;
        this.traceId = traceId;
        this.accountBalance = accountBalance;
        this.game = game;
        this.spinWheels = spinWheels;
        this.winInfo = winInfo;
        this.balanceTransfer = balanceTransfer;
        this.betMultiplier = betMultiplier;
        this.pendingSpins = pendingSpins;
        this.lock = lock;

        for (SpinWheel spinWheel : this.spinWheels) {
            spinWheel.addSpinCompleteListener(this::onSpinComplete);
        }
        this.accountBalance.setChangeListener(this::onAccountBalanceChanged);
        this.game.setRiskCompleteCallback(this::onRiskFinished);
        this.game.setRiskUnfreezeCallback(this::onRiskAnimationUnfreeze);
        this.winInfo.setHighlightCompleteCallback(this::onWinHighlightFinished);
        this.balanceTransfer.setTransferHandler(this::handleBalanceTransferTick);
    }

    public static PlayerGameInstance createForPlayer(ServerPlayer player, BlockPos position, SlotMachineConfig config) {
        List<SpinWheel> spinWheelList = new ArrayList<>(6);
        RandomSource random = player.getRandom();
        for (int i = 0; i < 6; i++) {
            GameType type = i < 3 ? GameType.LOW : GameType.HIGH;
            int generatorIdx = i % 3;
            List<String> sequence = config.generateSequence(random, type, generatorIdx);
            SpinWheel spinWheel = new SpinWheel(sequence, 0.0F, 0);
            spinWheelList.add(spinWheel);
        }
        Game game = Game.create();
        reloadSequences(spinWheelList.subList(0, 3), GameType.LOW, config, random, game);
        reloadSequences(spinWheelList.subList(3, 6), GameType.HIGH, config, random, game);

        UUID playerId = player.getUUID();
        String traceId = Phoenix.getTraceId(position, playerId);
        return new PlayerGameInstance(
                playerId,
                traceId,
                AccountBalance.createDefault(),
                game,
                spinWheelList,
                GameWinInfo.create(),
                BalanceTransfer.createInitial(),
                BetMultiplier.X1,
                0,
                Lock.EMPTY
        );
    }

    public void tick(PhoenixSlotMachineBlockEntity slotMachine) {
        int index = this.game.getSelectedGameType() == GameType.LOW ? 0 : 3;
        for (int i = index; i < index + 3; i++) {
            SpinWheel wheel = this.spinWheels.get(i);
            PhoenixConfig.SpinConfiguration configuration = index < 3 ? Phoenix.CONFIG.lowSpinConfig : Phoenix.CONFIG.highSpinConfig;
            wheel.update(slotMachine, configuration);
        }
        this.balanceTransfer.tick(slotMachine);
        this.game.update(slotMachine);
        this.winInfo.update(slotMachine);
        if (this.needsSynchronization) {
            this.updateSlotMachineAndView(slotMachine);
        }
    }

    public boolean canSpinOrTransfer() {
        if (this.isLocked()) {
            return false;
        }
        return this.hasSufficientBalanceForGame(this.game.getSelectedGameType()) || this.accountBalance.hasBalanceInAccount(AccountType.WIN);
    }

    public void startPlaying(Player player) {
        if (!this.canSpinOrTransfer()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Attempted to start playing which was not available due to restrictions", this.traceId);
            return;
        }
        this.winInfo.reset();
        if (this.accountBalance.getWinBalance() > 0) {
            Phoenix.LOGGER.debug(MARKER, "[{}] Risk game round skipped, transferring win balance of {} to multiWin account", this.traceId, this.accountBalance.getWinBalance());
            this.startBalanceTransfer(BalanceTransfer.InitiatorType.RISK_TRANSFER, AccountType.WIN, AccountType.MULTIWIN, this.accountBalance.getWinBalance());
        } else {
            Level level = player.level();
            level.playSound(null, player, Phoenix.SOUND_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            int balanceCost = this.getCost(GameType.LOW);
            this.accountBalance.subtractBalance(AccountType.INPUT, balanceCost);
            if (game.getSelectedGameType() == GameType.HIGH) {
                int balanceCostMultiWin = this.getCost(GameType.HIGH);
                this.accountBalance.subtractBalance(AccountType.MULTIWIN, balanceCostMultiWin);
            }
            List<SpinWheel> spinWheels = this.getSpinWheelsForGame(this.game.getSelectedGameType());
            reloadSequences(spinWheels, this.game.getSelectedGameType(), PhoenixSlotMachineBlockEntity.getConfig(), player.getRandom(), this.game);
            this.pendingSpins = spinWheels.size() - this.game.getHeldCount();
            Phoenix.LOGGER.debug(MARKER, "[{}] Starting spin round with {} active and {} held spin wheels on {} game type", this.traceId, this.pendingSpins, this.game.getHeldCount(), this.game.getSelectedGameType());
            this.lock(Lock.SPIN);
            if (this.game.getSelectedGameType().isLow())
                this.game.setPlayed(this.game.getHeldCount() <= 0);
            RandomSource random = player.getRandom();
            PhoenixConfig config = Phoenix.CONFIG;
            PhoenixConfig.SpinConfiguration spinConfiguration = this.game.getSelectedGameType().isLow() ? config.lowSpinConfig : config.highSpinConfig;
            int currentSpinDuration = spinConfiguration.minSpinDuration;
            for (int i = 0; i < spinWheels.size(); i++) {
                SpinWheel spinWheel = spinWheels.get(i);
                currentSpinDuration += (spinConfiguration.minAdditionalSpinDuration + random.nextInt(spinConfiguration.additionalSpinDuration));
                if (this.game.getSelectedGameType() == GameType.LOW && this.game.isHeld(i)) {
                    continue;
                }
                spinWheel.startSpinning(currentSpinDuration);
            }
        }
        this.game.cancelRisk();
    }

    public boolean canStartRisk() {
        return !this.isLocked() && this.game.isRiskActive() && !this.game.isFrozen() && this.accountBalance.hasBalanceInAccount(AccountType.WIN);
    }

    public void startRisk(Player player, boolean riskHearts) {
        if (!this.canStartRisk()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Attempted to start risk game which was not available", this.traceId);
            return;
        }
        RandomSource random = player.getRandom();
        PhoenixConfig config = Phoenix.CONFIG;
        int duration = config.minRiskDuration + random.nextInt(config.additionalRiskDuration);
        this.game.startRiskBet(duration, riskHearts);
        Phoenix.LOGGER.debug(MARKER, "[{}] Starting risk game with stop delay of {}. Bet on hearts: {}", this.traceId, duration, riskHearts);
        this.lock(Lock.RISK);
    }

    public boolean canSwapGameType() {
        if (this.isLocked() || this.game.getHeldCount() > 0 || this.game.isRiskActive())
            return false;
        if (this.game.getSelectedGameType().isHigh())
            return true;
        return this.hasSufficientBalanceForGame(GameType.HIGH);
    }

    public boolean hasSufficientBalanceForGame(GameType type) {
        return this.hasSufficientBalanceForGame(this.betMultiplier, type);
    }

    public boolean hasSufficientBalanceForGame(BetMultiplier bet, GameType type) {
        if (type.isHigh()) {
            int highCost = this.getCost(bet, type);
            if (!this.accountBalance.hasBalanceInAccount(AccountType.MULTIWIN, highCost)) {
                return false;
            }
        }
        return this.accountBalance.hasBalanceInAccount(AccountType.INPUT, this.getCost(bet, GameType.LOW));
    }

    public void swapGameType() {
        this.swapGameType(false);
    }

    public void swapGameType(boolean force) {
        if (!force && !this.canSwapGameType()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Failed to swap game type, not enough balance or already holding a slot", this.traceId);
            return;
        }
        this.game.changeGameType();
        this.winInfo.reset();
        Phoenix.LOGGER.debug(MARKER, "[{}] Swapped game type to {}", this.traceId, this.game.getSelectedGameType());
    }

    public boolean canHold(int slot) {
        return !this.isLocked() && this.game.getSelectedGameType().isLow() && this.game.hasPlayed() && this.game.getHeldCount() < 2 && !this.game.isHeld(slot) && this.hasSufficientBalanceForGame(this.game.getSelectedGameType());
    }

    public void hold(int slot) {
        if (!this.canHold(slot)) {
            Phoenix.LOGGER.error(MARKER, "[{}] Failed to lock slot {}, not enough slots available slots or already held", this.traceId, slot);
            return;
        }
        this.game.hold(slot);
        Phoenix.LOGGER.debug(MARKER, "[{}] Holding slot {} for next round, currently holding {} wheels", this.traceId, slot, this.game.getHeldCount());
    }

    public boolean canWithdrawMultiWin() {
        if (this.isLocked())
            return false;
        int amount = this.betMultiplier.getValue(Phoenix.CONFIG.multiWinSpinPriceMultiplier);
        return this.accountBalance.hasBalanceInAccount(AccountType.MULTIWIN, amount);
    }

    public void transferMultiWin() {
        int requestAmount = this.betMultiplier.getValue(Phoenix.CONFIG.multiWinSpinPriceMultiplier);
        Phoenix.LOGGER.debug(MARKER, "[{}] Attempting to withdraw {} from multiWin account", this.traceId, requestAmount);
        if (!this.canWithdrawMultiWin()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Failed to withdraw {} from multiWin account, not enough balance. Available balance: {}", this.traceId, requestAmount, this.accountBalance.getMultiWinBalance());
            return;
        }
        this.accountBalance.transferBalance(AccountType.MULTIWIN, AccountType.INPUT, requestAmount);
    }

    public AccountBalance getAccountBalance() {
        return accountBalance;
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

    public Game getGame() {
        return game;
    }

    public SpinWheel getSpinWheel(GameType type, int index) {
        int listIndex = (type.ordinal() * 3 + index) % this.spinWheels.size();
        return this.spinWheels.get(listIndex);
    }

    public List<SpinWheel> getSpinWheelsForGame(GameType type) {
        return this.spinWheels.subList(type.ordinal() * 3, (type.ordinal() + 1) * 3);
    }

    public boolean canToggleBetMultiplier() {
        if (this.isLocked() || this.game.getHeldCount() > 0 || this.game.isRiskActive()) {
            return false;
        }
        BetMultiplier nextBet = this.getNextBetMultiplier();
        return nextBet != null && this.hasSufficientBalanceForGame(nextBet, this.game.getSelectedGameType());
    }

    public @Nullable BetMultiplier getNextBetMultiplier() {
        int maxIterations = BetMultiplier.values().length - 1;
        BetMultiplier multiplier = this.betMultiplier;
        for (int i = 0; i < maxIterations; i++) {
            multiplier = EnumHelper.next(multiplier);
            if (this.hasSufficientBalanceForGame(multiplier, this.game.getSelectedGameType())) {
                return multiplier;
            }
        }
        return null;
    }

    public void toggleBetMultiplier() {
        if (!this.canToggleBetMultiplier()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Failed to toggle bet multiplier, not enough balance or already holding a slot", this.traceId);
            return;
        }
        BetMultiplier multiplier = this.getNextBetMultiplier();
        if (multiplier == null) {
            throw new IllegalStateException("Failed to find next bet multiplier");
        }
        Phoenix.LOGGER.debug(MARKER, "[{}] Bet multiplier changed {} -> {}", this.traceId, this.betMultiplier, multiplier);
        this.betMultiplier = multiplier;
    }

    public int getBetMultiplierValue() {
        return this.betMultiplier.getMultiplier();
    }

    public GameWinInfo getWinInfo() {
        return winInfo;
    }

    public void startBalanceTransfer(BalanceTransfer.InitiatorType initiatorType, AccountType targetAccount, int amount) {
        this.startBalanceTransfer(initiatorType, null, targetAccount, amount);
    }

    public void startBalanceTransfer(BalanceTransfer.InitiatorType initiatorType, @Nullable AccountType sourceAccount, AccountType targetAccount, int amount) {
        this.lock(initiatorType.getLock());
        int duration = this.getBalanceTransferDuration(initiatorType);
        this.balanceTransfer.initiate(this.traceId, Optional.ofNullable(sourceAccount), targetAccount, amount, duration, initiatorType);
    }

    public int getCost(GameType type) {
        return this.getCost(this.betMultiplier, type);
    }

    public int getCost(BetMultiplier bet, GameType type) {
        int cost = 1;
        if (type.isHigh())
            cost *= Phoenix.CONFIG.multiWinSpinPriceMultiplier;
        return bet.getValue(cost);
    }

    public boolean isRolling() {
        return this.pendingSpins > 0;
    }

    public PlayerGameInstance update(PlayerGameInstance holder) {
        this.accountBalance.updateFrom(holder.accountBalance);
        this.game.updateFrom(holder.game);
        this.winInfo.update(holder.winInfo);
        this.balanceTransfer.update(holder.balanceTransfer);
        this.betMultiplier = holder.betMultiplier;
        this.pendingSpins = holder.pendingSpins;
        this.lock = holder.lock;
        for (int i = 0; i < Math.min(this.spinWheels.size(), holder.spinWheels.size()); i++) {
            this.spinWheels.get(i).updateFrom(holder.spinWheels.get(i));
        }
        return this;
    }

    private void onSpinComplete(PhoenixSlotMachineBlockEntity slotMachine, float amount) {
        Phoenix.LOGGER.debug(MARKER, "[{}] Spin completed, remaining wheels: {}", this.traceId, this.pendingSpins - 1);
        Level level = slotMachine.getLevel();
        BlockPos pos = slotMachine.getBlockPos();
        level.playSound(null, pos, Phoenix.SOUND_SLOT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (--this.pendingSpins <= 0) {
            Phoenix.LOGGER.debug(MARKER, "[{}] All spin wheels finished, checking winning combination for {} game type", this.traceId, this.game.getSelectedGameType());
            SlotMachineConfig config = PhoenixSlotMachineBlockEntity.getConfig();
            GameType gameType = this.game.getSelectedGameType();
            WinConfigurationConfig winConfiguration = config.getWinningConfiguration();
            List<SpinWheel> spinWheels = this.getSpinWheelsForGame(gameType);
            List<MatchedWinCombination> wins = winConfiguration.resolveWins(gameType, spinWheels);
            this.winInfo.assignWinCombination(wins);
            if (!wins.isEmpty()) {
                Phoenix.LOGGER.debug(MARKER, "[{}] Found {} winning combinations, freezing hold for next round. Combinations: {}", this.traceId, wins.size(), wins);
                this.game.setPlayed(false);
            } else {
                Phoenix.LOGGER.debug(MARKER, "[{}] No winning combinations found, clearing held slot", this.traceId);
                this.game.clearHold();
                this.unlock(Lock.SPIN);
            }
            if (this.accountBalance.getInputBalance() <= 0) {
                Phoenix.LOGGER.debug(MARKER, "[{}] No more input balance, disabling held slots", this.traceId);
                this.game.setPlayed(false);
            }
        }
        this.markForUpdate();
    }

    private void onRiskFinished(PhoenixSlotMachineBlockEntity slotMachine, boolean won) {
        Phoenix.LOGGER.debug(MARKER, "[{}] Risk game finished with win: {}", this.traceId, won);
        int wonBalance = won ? this.accountBalance.getWinBalance() * (Phoenix.CONFIG.riskGameWinMultiplier - 1) : 0;
        if (wonBalance > 0) {
            Phoenix.LOGGER.debug(MARKER, "[{}] Risk game won, bonus balance: {}", this.traceId, wonBalance);
            this.accountBalance.addBalance(Phoenix.CONFIG.riskGameTargetAccount, wonBalance);
        } else {
            Phoenix.LOGGER.debug(MARKER, "[{}] Risk game lost, removing win bet balance", this.traceId);
            this.accountBalance.clearBalance(AccountType.WIN);
            this.game.cancelRisk();
            this.winInfo.reset();
        }
        this.markForUpdate();
    }

    private static void reloadSequences(List<SpinWheel> wheels, GameType gameType, SlotMachineConfig config, RandomSource random, Game game) {
        for (int i = 0; i < wheels.size(); i++) {
            boolean isHeld = gameType == GameType.LOW && game.isHeld(i);
            if (isHeld)
                continue;
            SpinWheel wheel = wheels.get(i);
            List<String> sequence = config.generateSequence(random, gameType, i);
            wheel.setSequence(sequence);
        }
    }

    private void onWinHighlightFinished(PhoenixSlotMachineBlockEntity slotMachine) {
        MatchedWinCombination winCombination = this.winInfo.getAnimatedWinCombination();
        AccountType targetAccount = this.game.getSelectedGameType().isHigh() ? Phoenix.CONFIG.highGameTargetAccount : Phoenix.CONFIG.lowGameTargetAccount;
        int winAmount = this.betMultiplier.getValue(winCombination.amount());
        this.startBalanceTransfer(BalanceTransfer.InitiatorType.SPIN, targetAccount, winAmount);
        this.winInfo.transitionToBlinkMode();
        this.markForUpdate();
    }

    private void handleBalanceTransferTick(PhoenixSlotMachineBlockEntity slotMachine, BalanceTransfer.InitiatorType initiatorType, int amount, int remaining, @Nullable AccountType source, AccountType targetAccount) {
        this.accountBalance.addBalance(targetAccount, amount);
        if (source != null) {
            this.accountBalance.subtractBalance(source, amount);
        }
        if (remaining <= 0) {
            Phoenix.LOGGER.debug(MARKER, "[{}] Balance transfer finished from source {}", this.traceId, initiatorType);
            if (this.getLockReason() != LockReason.SPIN) {
                this.unlock(initiatorType.getLock());
            }
            if (initiatorType == BalanceTransfer.InitiatorType.SPIN) {
                this.winInfo.cancelBlinkMode();
                this.game.clearHold();
                if (this.winInfo.isFinalAnimation()) {
                    this.winInfo.forceLockAnimation();
                    if (this.accountBalance.getWinBalance() > 0) {
                        this.game.enableRisk();
                        Phoenix.LOGGER.debug(MARKER, "[{}] Detected {} winning balance, activating risk game", this.traceId, this.accountBalance.getWinBalance());
                    }
                    if (this.game.getSelectedGameType().isHigh() && !this.hasSufficientBalanceForGame(GameType.HIGH)) {
                        this.swapGameType(true);
                    }
                    this.unlock(Lock.SPIN);
                }
            }
        }
        this.markForUpdate();
    }

    private void onRiskAnimationUnfreeze(PhoenixSlotMachineBlockEntity slotMachineBlock) {
        this.unlock(Lock.RISK);
        this.markForUpdate();
    }

    private int getBalanceTransferDuration(BalanceTransfer.InitiatorType initiatorType) {
        return switch (initiatorType) {
            case SPIN -> this.betMultiplier.getBalanceTransferDuration();
            case RISK_TRANSFER -> 50;
            case PENDING -> throw new IllegalArgumentException("Invalid initiator type for balance transfer: " + initiatorType);
        };
    }

    private void onAccountBalanceChanged(AccountType type, int originalAmount, int newAmount) {
        int diff = newAmount - originalAmount;
        String diffLabel = (diff > 0 ? "+" : "") + diff;
        Phoenix.LOGGER.debug(MARKER, "[{}] Balance changed in account {}: {} -> {} [{}]", this.traceId, type, originalAmount, newAmount, diffLabel);
        this.markForUpdate();
    }

    private void markForUpdate() {
        this.needsSynchronization = true;
    }

    private void updateSlotMachineAndView(PhoenixSlotMachineBlockEntity slotMachine) {
        slotMachine.markUpdated();
        Level level = slotMachine.getLevel();
        Player player = level.getPlayerByUUID(this.owner);
        if (player != null) {
            slotMachine.updatePlayerView(player);
        }
        this.needsSynchronization = false;
    }
}
