package dev.tnt.phoenix.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.AccountBalance;
import dev.tnt.phoenix.api.AccountType;
import dev.tnt.phoenix.api.RiskBet;
import dev.tnt.phoenix.api.RiskGame;
import dev.tnt.phoenix.config.PhoenixConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public final class RiskGameComponent extends PhoenixComponent implements RiskGame {

    public static final Marker MARKER = MarkerManager.getMarker("RiskGame");
    public static final int RISK_RESULT_FREEZE_DURATION = 25;

    public static final Codec<RiskGameComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(t -> t.enabled),
            Codec.INT.optionalFieldOf("game_tick", 0).forGetter(t -> t.gameTick),
            Codec.INT.optionalFieldOf("stop_delay", 0).forGetter(t -> t.stopDelay),
            Codec.INT.optionalFieldOf("freeze_duration", 0).forGetter(t -> t.freezeDuration),
            RiskBet.CODEC.optionalFieldOf("bet", RiskBet.NONE).forGetter(t -> t.bet),
            Codec.INT.optionalFieldOf("streak", 0).forGetter(t -> t.winStreak)
    ).apply(instance, RiskGameComponent::new));

    private boolean enabled;
    private int gameTick;
    private int stopDelay;
    private int freezeDuration;
    private RiskBet bet;
    private int winStreak;

    private RiskGameComponent(boolean enabled, int gameTick, int stopDelay, int freezeDuration, RiskBet riskBet, int winStreak) {
        this.enabled = enabled;
        this.gameTick = gameTick;
        this.stopDelay = stopDelay;
        this.freezeDuration = freezeDuration;
        this.bet = riskBet;
        this.winStreak = winStreak;
    }

    public static RiskGameComponent initComponent() {
        return new RiskGameComponent(false, 0, 0, 0, RiskBet.NONE, 0);
    }

    @Override
    public boolean isActive() {
        return this.enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void stop() {
        this.enabled = false;
        this.winStreak = 0;
    }

    public void tick(Level level, BlockPos pos) {
        if (this.freezeDuration > 0) {
            if (--this.freezeDuration <= 0) {
                Phoenix.LOGGER.debug(MARKER, "[{}] Risk result freeze finished", this.instanceAccess.traceId());
                this.instanceAccess.unlock(LOCK);
                this.instanceAccess.setChanged();
            }
            return;
        }
        if (this.enabled) {
            ++this.gameTick;
            if (this.stopDelay > 0 && --this.stopDelay <= 0) {
                this.onRiskFinished(level, pos);
            }
        }
    }

    public void updateFrom(RiskGameComponent other) {
        this.enabled = other.enabled;
        this.gameTick = other.gameTick;
        this.stopDelay = other.stopDelay;
        this.freezeDuration = other.freezeDuration;
        this.bet = other.bet;
        this.winStreak = other.winStreak;
    }

    public boolean canStart() {
        AccountBalance accountBalance = this.instanceAccess.account();
        return !this.instanceAccess.isLocked() && this.enabled && !this.isStopped() && accountBalance.hasBalanceInAccount(AccountType.WIN);
    }

    public void start(RandomSource random, RiskBet bet) {
        if (!this.canStart()) {
            Phoenix.LOGGER.error(MARKER, "[{}] Attempted to start risk game which was not available", this.instanceAccess.traceId());
            return;
        }
        PhoenixConfig config = Phoenix.CONFIG;
        int duration = config.minRiskDuration + random.nextInt(config.additionalRiskDuration);
        Phoenix.LOGGER.debug(MARKER, "[{}] Starting risk game with stop delay of {}. Bet on: {}", this.instanceAccess.traceId(), duration, bet);
        this.stopDelay = duration;
        this.bet = bet;
        this.instanceAccess.lock(LOCK);
    }

    public RiskBet resolveWinningBet() {
        return RiskBet.fromValue(this.gameTick);
    }

    public RiskBet getBet() {
        return bet;
    }

    @Override
    public boolean isStopped() {
        return this.freezeDuration > 0;
    }

    private void onRiskFinished(Level level, BlockPos pos) {
        this.freezeDuration = RISK_RESULT_FREEZE_DURATION;
        RiskBet winningBet = this.resolveWinningBet();
        Phoenix.LOGGER.debug(MARKER, "[{}] Risk bet finished. Winning bet: {}", this.instanceAccess.traceId(), winningBet);
        if (winningBet == this.bet) {
            this.onRiskWon(level, pos);
        } else {
            this.onRiskLost(level, pos);
        }
        this.instanceAccess.setChanged();
    }

    private void onRiskWon(Level level, BlockPos pos) {
        PhoenixConfig config = Phoenix.CONFIG;
        AccountBalance balance = this.instanceAccess.account();

        float pitch = 1.0F + this.winStreak * 0.05F;
        level.playSound(null, pos, Phoenix.SOUND_GAMBLE_WIN.get(), SoundSource.BLOCKS, 0.3F, pitch);

        ++this.winStreak;
        int multiplier = config.riskGameWinMultiplier - 1;
        int winningBalance = balance.getBalance(AccountType.WIN) * multiplier; // TODO streak multiplier

        Phoenix.LOGGER.debug(MARKER, "[{}] Risk bet won, adding balance {}. Current win streak: {}", this.instanceAccess.traceId(), winningBalance, this.winStreak);
        balance.addBalance(config.riskGameTargetAccount, winningBalance);
    }

    private void onRiskLost(Level level, BlockPos pos) {
        this.winStreak = 0;
        Phoenix.LOGGER.debug(MARKER, "[{}] Risk bet lost, removing winning balance", this.instanceAccess.traceId());
        AccountBalance balance = this.instanceAccess.account();
        balance.clearBalance(AccountType.WIN);

        level.playSound(null, pos, Phoenix.SOUND_GAMBLE_LOSE.get(), SoundSource.BLOCKS, 0.3F, 1.0F);

        this.stop();
    }
}
