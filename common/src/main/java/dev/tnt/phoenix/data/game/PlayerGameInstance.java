package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.data.GameType;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.WinCombination;
import dev.tnt.phoenix.data.WinConfigurationConfig;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerGameInstance {

    public static final Codec<PlayerGameInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(t -> t.owner),
            AccountBalance.CODEC.fieldOf("account_balance").forGetter(PlayerGameInstance::getAccountBalance),
            Game.CODEC.fieldOf("active_spin").forGetter(t -> t.game),
            SpinWheel.CODEC.listOf().fieldOf("spin_wheels").forGetter(t -> t.spinWheels),
            BetMultiplier.CODEC.optionalFieldOf("bet_multiplier", BetMultiplier.X1).forGetter(t -> t.betMultiplier),
            Codec.INT.optionalFieldOf("spins", 0).forGetter(t -> t.pendingSpins),
            Lock.CODEC.optionalFieldOf("lock", Lock.EMPTY).forGetter(t -> t.lock)
    ).apply(instance, PlayerGameInstance::new));

    private final UUID owner;
    private final AccountBalance accountBalance;
    private final Game game;
    private final List<SpinWheel> spinWheels;
    private BetMultiplier betMultiplier;
    private int pendingSpins;
    private Lock lock;

    private PlayerGameInstance(UUID owner, AccountBalance accountBalance, Game game, List<SpinWheel> spinWheels, BetMultiplier betMultiplier, int pendingSpins, Lock lock) {
        this.owner = owner;
        this.accountBalance = accountBalance;
        this.game = game;
        this.spinWheels = spinWheels;
        this.betMultiplier = betMultiplier;
        this.pendingSpins = pendingSpins;
        this.lock = lock;

        for (SpinWheel spinWheel : this.spinWheels) {
            spinWheel.addSpinCompleteListener(this::onSpinComplete);
        }
        this.game.addRiskCompleteListener(this::onRiskFinished);
    }

    public static PlayerGameInstance createForPlayer(ServerPlayer player, SlotMachineConfig config) {
        List<SpinWheel> spinWheelList = new ArrayList<>(6);
        RandomSource random = player.getRandom();
        for (int i = 0; i < 6; i++) {
            GameType type = i < 3 ? GameType.LOW : GameType.HIGH;
            int generatorIdx = i % 3;
            List<String> sequence = config.generateSequence(random, type, generatorIdx);
            SpinWheel spinWheel = new SpinWheel(sequence, 0.0F, 0);
            spinWheelList.add(spinWheel);
        }
        reloadSequences(spinWheelList.subList(0, 3), GameType.LOW, config, random);
        reloadSequences(spinWheelList.subList(3, 6), GameType.HIGH, config, random);
        return new PlayerGameInstance(
                player.getUUID(),
                AccountBalance.createDefault(),
                Game.create(),
                spinWheelList,
                BetMultiplier.X1,
                0,
                Lock.EMPTY
        );
    }

    public void tick(PhoenixSlotMachineBlockEntity slotMachine) {
        int index = this.game.getSelectedGameType() == GameType.LOW ? 0 : 3;
        for (int i = index; i < index + 3; i++) {
            SpinWheel wheel = this.spinWheels.get(i);
            wheel.update(slotMachine);
        }
        this.game.update(slotMachine);
    }

    public void startPlaying(PhoenixSlotMachineBlockEntity slotMachine, Player player) {
        int balanceCost = this.getCost(GameType.LOW);
        this.accountBalance.subtractBalance(BalanceType.INPUT, balanceCost);
        this.accountBalance.transferBalance(BalanceType.WIN, BalanceType.MULTIWIN);
        if (game.getSelectedGameType() == GameType.HIGH) {
            int balanceCostMultiWin = this.getCost(GameType.HIGH);
            this.accountBalance.subtractBalance(BalanceType.MULTIWIN, balanceCostMultiWin);
        }
        List<SpinWheel> spinWheels = this.getSpinWheelsForGame(this.game.getSelectedGameType());
        reloadSequences(spinWheels, this.game.getSelectedGameType(), PhoenixSlotMachineBlockEntity.getConfig(), player.getRandom());
        this.pendingSpins = spinWheels.size(); // TODO subtract held amount
        this.lock(Lock.SPIN);
        RandomSource random = player.getRandom();
        int currentSpinDuration = 30;
        for (SpinWheel spinWheel : spinWheels) {
            currentSpinDuration += (5 + random.nextInt(15));
            // TODO skip if held
            spinWheel.startSpinning(currentSpinDuration);
        }
    }

    public void startRisk(Player player, boolean riskHearts) {
        RandomSource random = player.getRandom();
        int duration = 35 + random.nextInt(65);
        this.game.startRisk(duration, riskHearts);
        this.lock(Lock.RISK);
    }

    public AccountBalance getAccountBalance() {
        return accountBalance;
    }

    public void lock(Lock lock) {
        this.lock = lock;
    }

    public void unlock() {
        this.unlock(null);
    }

    public void unlock(@Nullable Lock lock) {
        if (lock == null || this.lock.equals(lock)) {
            this.lock(Lock.EMPTY);
        }
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

    public void toggleBetMultiplier() {
        this.betMultiplier = this.betMultiplier.next();
    }

    public void setBetMultiplier(BetMultiplier betMultiplier) {
        this.betMultiplier = betMultiplier;
    }

    public int getBetMultiplierValue() {
        return this.betMultiplier.getMultiplier();
    }

    public BetMultiplier getBetMultiplier() {
        return betMultiplier;
    }

    public int getCost(GameType type) {
        int baseCost = switch (type) {
            case LOW -> 1;
            case HIGH -> 4;
        };
        return this.betMultiplier.getValue(baseCost);
    }

    public PlayerGameInstance update(PlayerGameInstance holder) {
        this.accountBalance.updateFrom(holder.accountBalance);
        this.game.updateFrom(holder.game);
        this.betMultiplier = holder.betMultiplier;
        this.pendingSpins = holder.pendingSpins;
        this.lock = holder.lock;
        for (int i = 0; i < Math.min(this.spinWheels.size(), holder.spinWheels.size()); i++) {
            this.spinWheels.get(i).updateFrom(holder.spinWheels.get(i));
        }
        return this;
    }

    private void onSpinComplete(PhoenixSlotMachineBlockEntity slotMachine, float amount) {
        if (--this.pendingSpins <= 0) {
            SlotMachineConfig config = PhoenixSlotMachineBlockEntity.getConfig();
            GameType gameType = this.game.getSelectedGameType();
            WinConfigurationConfig winConfiguration = config.getWinningConfiguration();
            List<SpinWheel> spinWheels = this.getSpinWheelsForGame(gameType);
            List<WinCombination> wins = winConfiguration.resolveWins(gameType, spinWheels);
            for (WinCombination winningCombination : wins) {
                Phoenix.LOGGER.debug("Winning combination match found: {}", winningCombination);
                this.accountBalance.addBalance(BalanceType.WIN, this.betMultiplier.getValue(winningCombination.amount()));
            }
            this.unlock(Lock.SPIN);
        }
        this.updateSlotMachineAndView(slotMachine);
    }

    private void onRiskFinished(PhoenixSlotMachineBlockEntity slotMachine, boolean won) {
        this.unlock(Lock.RISK);
        if (won) {
            this.accountBalance.multiplyBalance(BalanceType.WIN, 2);
        } else {
            this.accountBalance.clearBalance(BalanceType.WIN);
        }
        this.updateSlotMachineAndView(slotMachine);
    }

    private static void reloadSequences(List<SpinWheel> wheels, GameType gameType, SlotMachineConfig config, RandomSource random) {
        for (int i = 0; i < wheels.size(); i++) {
            SpinWheel wheel = wheels.get(i);
            List<String> sequence = config.generateSequence(random, gameType, i);
            wheel.setSequence(sequence);
        }
    }

    private void updateSlotMachineAndView(PhoenixSlotMachineBlockEntity slotMachine) {
        slotMachine.markUpdated();
        Level level = slotMachine.getLevel();
        Player player = level.getPlayerByUUID(this.owner);
        if (player != null) {
            slotMachine.updatePlayerView(player);
        }
    }
}
