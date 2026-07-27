package dev.tnt.phoenix.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.*;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.config.PhoenixConfig;
import dev.tnt.phoenix.data.*;
import dev.tnt.phoenix.util.EnumHelper;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class SpinGameComponent extends PhoenixComponent implements SpinGame {

    public static final Marker MARKER = MarkerManager.getMarker("SpinGame");

    public static final Codec<SpinGameComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(GameType.CODEC, SpinWheel.CODEC.listOf()).fieldOf("wheels").forGetter(t -> t.wheels),
            Codec.INT.optionalFieldOf("active_wheels", 0).forGetter(t -> t.activeWheels),
            GameType.CODEC.optionalFieldOf("active_game_type", GameType.LOW).forGetter(t -> t.gameType),
            Bet.CODEC.optionalFieldOf("bet", Bet.X1).forGetter(t -> t.bet),
            GameWinInfo.CODEC.optionalFieldOf("win_info", GameWinInfo.create()).forGetter(t -> t.winInfo),
            Codec.BOOL.optionalFieldOf("hold_enabled", false).forGetter(t -> t.holdEnabled),
            Codec.INT.listOf().optionalFieldOf("hold", Collections.emptyList()).forGetter(t -> new ArrayList<>(t.hold))
    ).apply(instance, SpinGameComponent::new));

    private final Map<GameType, List<SpinWheel>> wheels;
    private int activeWheels;
    private GameType gameType;
    private Bet bet;
    private final GameWinInfo winInfo;
    private boolean holdEnabled;
    private final IntSet hold;

    private SpinGameComponent(Map<GameType, List<SpinWheel>> wheels, int activeWheels, GameType gameType, Bet betMultiplier, GameWinInfo winInfo, boolean holdEnabled, List<Integer> hold) {
        this.wheels = new EnumMap<>(wheels);
        this.activeWheels = activeWheels;
        this.gameType = gameType;
        this.bet = betMultiplier;
        this.winInfo = winInfo;
        this.holdEnabled = holdEnabled;
        this.hold = new IntOpenHashSet(hold);

        this.wheels.values().stream()
                .flatMap(Collection::stream)
                .forEach(wheel -> wheel.setFinishCallback(this::onSpinFinished));
        this.winInfo.setHighlightCompleteCallback(this::onWinHighlightFinished);
    }

    public static SpinGameComponent initComponent(RandomSource random) {
        SlotMachineConfig slotMachineConfig = PhoenixSlotMachineBlockEntity.getConfig();
        Map<GameType, List<SpinWheel>> wheels = new EnumMap<>(GameType.class);
        for (GameType type : GameType.values()) {
            List<SpinWheel> wheelList = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                List<String> sequence = slotMachineConfig.generateSequence(random, type, i);
                wheelList.add(new SpinWheel(sequence, 0.0F, 0, 1.0F));
            }
            wheels.put(type, wheelList);
        }

        return new SpinGameComponent(
                wheels, 0, GameType.LOW, Bet.X1, GameWinInfo.create(), false, Collections.emptyList()
        );
    }

    public void tick(Level level, BlockPos pos) {
        List<SpinWheel> activeWheels = this.getSpinWheelsForGame();
        for (SpinWheel wheel : activeWheels) {
            wheel.update(level, pos);
        }
        this.winInfo.tick(level, pos);
    }

    public void updateFrom(SpinGameComponent holder) {
        for (var entry : holder.wheels.entrySet()) {
            List<SpinWheel> source = entry.getValue();
            List<SpinWheel> dest = this.wheels.get(entry.getKey());
            for (int i = 0; i < Math.min(source.size(), dest.size()); i++) {
                dest.get(i).updateFrom(source.get(i));
            }
        }
        this.activeWheels = holder.activeWheels;
        this.gameType = holder.gameType;
        this.bet = holder.bet;
        this.winInfo.updateFrom(holder.winInfo);
        this.holdEnabled = holder.holdEnabled;
        this.hold.clear();
        this.hold.addAll(holder.hold);
    }

    public void start(Player player) {
        if (!this.canSpinOrTransfer()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Attempted to start playing which was not available due to restrictions", this.instanceAccess.traceId());
            return;
        }
        AccountBalance accountBalance = this.instanceAccess.account();
        AccountBalanceTransaction transaction = this.instanceAccess.transactions();

        // Stop animation
        this.winInfo.reset();

        int winAccountBalance = accountBalance.getBalance(AccountType.WIN);
        if (winAccountBalance > 0) {
            // Win balance exists, transfer only
            Phoenix.LOGGER.debug(MARKER, "[{}] Risk game round skipped, transferring win balance of {} to multiWin account", this.instanceAccess.traceId(), winAccountBalance);
            transaction.initiate(TransactionSource.RISK_TRANSFER, AccountType.WIN, AccountType.MULTIWIN, winAccountBalance);
        } else {
            // Spin initiation logic
            this.playStartSound(player);

            // Low game balance reduction
            int spinCost = this.getSpinCost(GameType.LOW);
            accountBalance.subtractBalance(AccountType.INPUT, spinCost);

            // High game balance reduction
            if (this.gameType.isHigh()) {
                int balanceCostMultiWin = this.getSpinCost();
                accountBalance.subtractBalance(AccountType.MULTIWIN, balanceCostMultiWin);
            }

            // Reload sequences on active wheels
            RandomSource random = player.getRandom();
            List<SpinWheel> spinWheels = this.getSpinWheelsForGame();
            this.updateSequences(random);
            this.activeWheels = spinWheels.size() - this.hold.size();

            // Lock slot machine
            Phoenix.LOGGER.debug(MARKER, "[{}] Starting spin round with {} active and {} held spin wheels on {} game type", this.instanceAccess.traceId(), this.activeWheels, this.hold.size(), this.gameType);
            this.instanceAccess.lock(LOCK);

            // Cancel hold for next round
            if (this.gameType.isLow()) {
                this.holdEnabled = this.hold.isEmpty();
            }

            // Spin
            this.beginSpin(spinWheels, random);
        }

        // Stop risk animation
        RiskGame riskGame = this.instanceAccess.risk();
        riskGame.stop();
    }

    @Override
    public Bet bet() {
        return this.bet;
    }

    public int getBetValue() {
        return this.bet.multiplier();
    }

    @Override
    public GameType gameType() {
        return this.gameType;
    }

    public GameWinInfo getWinInfo() {
        return this.winInfo;
    }

    public SpinWheel getWheel(GameType type, int pos) {
        return this.getSpinWheelsForGame(type).get(pos);
    }

    @Override
    public boolean isRolling() {
        return this.activeWheels > 0;
    }

    public boolean canSwapGameType() {
        if (this.instanceAccess.isLocked())
            return false;
        if (this.gameType.isHigh())
            return true;
        return this.hasSufficientBalanceForGame(GameType.HIGH);
    }

    @Override
    public int getSpinCost(Bet bet, GameType gameType) {
        int baseCost = 1;
        if (gameType.isHigh()) {
            baseCost *= Phoenix.CONFIG.multiWinSpinPriceMultiplier;
        }
        return bet.getValue(baseCost);
    }

    public boolean canSpinOrTransfer() {
        if (this.instanceAccess.isLocked()) {
            return false;
        }
        AccountBalance balance = this.instanceAccess.account();
        return this.hasSufficientBalanceForGame(this.gameType) || balance.hasBalanceInAccount(AccountType.WIN);
    }

    public void swapGameType(boolean force) {
        if (!force && !this.canSwapGameType()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Failed to swap game type, not enough balance or already holding a slot", this.instanceAccess.traceId());
            return;
        }
        this.gameType = EnumHelper.next(this.gameType);
        this.winInfo.reset();
        Phoenix.LOGGER.debug(MARKER, "[{}] Swapped game type to {}", this.instanceAccess.traceId(), this.gameType);
    }

    public boolean canToggleBet() {
        RiskGame riskGame = this.instanceAccess.risk();
        if (this.instanceAccess.isLocked() || !this.hold.isEmpty() || riskGame.isActive()) {
            return false;
        }
        Bet nextBet = this.getNextBetMultiplier();
        return nextBet != null && this.hasSufficientBalanceForGame(nextBet, this.gameType);
    }

    public void toggleBet() {
        if (!this.canToggleBet()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Failed to toggle bet multiplier, not enough balance or already holding a slot", this.instanceAccess.traceId());
            return;
        }
        Bet newBet = this.getNextBetMultiplier();
        if (newBet == null) {
            throw new IllegalStateException("Failed to find next bet multiplier");
        }
        Phoenix.LOGGER.debug(MARKER, "[{}] Bet multiplier changed {} -> {}", this.instanceAccess.traceId(), this.bet, newBet);
        this.bet = newBet;
    }

    public boolean canHold(int slot) {
        return !this.instanceAccess.isLocked() && this.gameType.isLow() && this.holdEnabled && this.hold.size() < 2 && !this.hold.contains(slot) && this.hasSufficientBalanceForGame(this.gameType);
    }

    public void hold(int slot) {
        if (!this.canHold(slot)) {
            Phoenix.LOGGER.error(MARKER, "[{}] Failed to lock slot {}, not enough slots available slots or already held", this.instanceAccess.traceId(), slot);
            return;
        }
        this.hold.add(slot);
        Phoenix.LOGGER.debug(MARKER, "[{}] Holding slot {} for next round, currently holding {} wheels", this.instanceAccess.traceId(), slot, this.hold.size());
    }

    public boolean isWheelHeld(int slot) {
        return this.hold.contains(slot);
    }

    public void onTransactionCompleted() {
        this.winInfo.cancelBlinkMode();
        this.hold.clear();
        if (this.winInfo.isFinalAnimation()) {
            this.winInfo.forceLockAnimation();
            AccountBalance balance = this.instanceAccess.account();
            int winningBalance = balance.getBalance(AccountType.WIN);
            if (winningBalance > 0) {
                RiskGame riskGame = this.instanceAccess.risk();
                riskGame.enable();
                Phoenix.LOGGER.debug(MARKER, "[{}] Detected {} winning balance, activating risk game", this.instanceAccess.traceId(), winningBalance);
            }
            if (this.gameType.isHigh() && !this.hasSufficientBalanceForGame(GameType.HIGH)) {
                this.swapGameType(true);
            }
            this.instanceAccess.unlock(LOCK);
        }
    }

    private boolean hasSufficientBalanceForGame(GameType type) {
        return this.hasSufficientBalanceForGame(this.bet, type);
    }

    private boolean hasSufficientBalanceForGame(Bet bet, GameType type) {
        AccountBalance accountBalance = this.instanceAccess.account();
        if (type.isHigh()) {
            int highCost = this.getSpinCost(bet, type);
            if (!accountBalance.hasBalanceInAccount(AccountType.MULTIWIN, highCost)) {
                return false;
            }
        }
        int lowGameSpinCost = this.getSpinCost(GameType.LOW);
        return accountBalance.hasBalanceInAccount(AccountType.INPUT, lowGameSpinCost);
    }

    private @Nullable Bet getNextBetMultiplier() {
        int maxIterations = Bet.values().length - 1;
        Bet multiplier = this.bet;
        for (int i = 0; i < maxIterations; i++) {
            multiplier = EnumHelper.next(multiplier);
            if (this.hasSufficientBalanceForGame(multiplier, this.gameType)) {
                return multiplier;
            }
        }
        return null;
    }

    private List<SpinWheel> getSpinWheelsForGame(GameType gameType) {
        return this.wheels.get(gameType);
    }

    private List<SpinWheel> getSpinWheelsForGame() {
        return this.getSpinWheelsForGame(this.gameType);
    }

    private void updateSequences(RandomSource random) {
        List<SpinWheel> list = this.getSpinWheelsForGame();
        SlotMachineConfig config = PhoenixSlotMachineBlockEntity.getConfig();
        for (int i = 0; i < list.size(); i++) {
            if (this.gameType.isLow() && this.hold.contains(i)) {
                continue;
            }
            SpinWheel wheel = list.get(i);
            wheel.reloadSequence(random, config, this.gameType, i);
        }
    }

    private void beginSpin(List<SpinWheel> wheels, RandomSource random) {
        PhoenixConfig.SpinConfiguration spinConfiguration = Phoenix.CONFIG.getSpinConfiguration(this.gameType);
        int currentSpinDuration = spinConfiguration.minSpinDuration;
        for (int i = 0; i < wheels.size(); i++) {
            SpinWheel spinWheel = wheels.get(i);
            currentSpinDuration += (spinConfiguration.minAdditionalSpinDuration + random.nextInt(spinConfiguration.additionalSpinDuration));
            if (this.gameType.isLow() && this.hold.contains(i)) {
                continue;
            }
            spinWheel.startSpinning(currentSpinDuration, spinConfiguration.spinSpeed);
        }
    }

    private void playStartSound(Entity source) {
        Level level = source.level();
        level.playSound(null, source, Phoenix.SOUND_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void playSlotSound(Level level, BlockPos pos) {
        level.playSound(null, pos, Phoenix.SOUND_SLOT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void onSpinFinished(Level level, BlockPos pos) {
        Phoenix.LOGGER.debug(MARKER, "[{}] Spin completed, remaining wheels: {}", this.instanceAccess.traceId(), this.activeWheels - 1);
        // Slot lock sound
        this.playSlotSound(level, pos);

        if (--this.activeWheels <= 0) {
            // Win check
            Phoenix.LOGGER.debug(MARKER, "[{}] All spin wheels finished, checking winning combination for {} game type", this.instanceAccess.traceId(), this.gameType);
            SlotMachineConfig config = PhoenixSlotMachineBlockEntity.getConfig();
            WinConfigurationConfig winConfiguration = config.getWinningConfiguration();
            List<SpinWheel> spinWheels = this.getSpinWheelsForGame();
            List<MatchedWinCombination> wins = winConfiguration.resolveWins(this.gameType, spinWheels);

            // Win animation
            this.winInfo.assignWinCombination(wins);
            if (!wins.isEmpty())
                this.winInfo.playHitSound(level, pos);

            if (!wins.isEmpty()) {
                // Won, locking hold for next round
                Phoenix.LOGGER.debug(MARKER, "[{}] Found {} winning combinations, freezing hold for next round. Combinations: {}", this.instanceAccess.traceId(), wins.size(), wins);
                this.holdEnabled = false;
            } else {
                // Lost, clear hold and unlock slot machine
                Phoenix.LOGGER.debug(MARKER, "[{}] No winning combinations found, clearing held slot", this.instanceAccess.traceId());
                this.hold.clear();
                this.instanceAccess.unlock(LOCK);
            }

            // No input balance, disable hold for next round
            if (!this.instanceAccess.account().hasBalanceInAccount(AccountType.INPUT)) {
                Phoenix.LOGGER.debug(MARKER, "[{}] No more input balance, disabling held slots", this.instanceAccess.traceId());
                this.holdEnabled = false;
            }
        }
        this.instanceAccess.setChanged();
    }

    private void onWinHighlightFinished() {
        MatchedWinCombination winCombination = this.winInfo.getAnimatedWinCombination();
        AccountType targetAccount = this.gameType.isHigh() ? Phoenix.CONFIG.highGameTargetAccount : Phoenix.CONFIG.lowGameTargetAccount;
        int winAmount = this.bet.getValue(winCombination.amount());
        AccountBalanceTransaction transaction = this.instanceAccess.transactions();
        transaction.initiate(TransactionSource.SPIN, targetAccount, winAmount);
        this.winInfo.transitionToBlinkMode();
        this.instanceAccess.setChanged();
    }

    @FunctionalInterface
    public interface SpinFinishCallback {
        void onSpinFinished(Level level, BlockPos pos);
    }
}
