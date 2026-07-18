package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.data.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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
            Codec.INT.optionalFieldOf("spins", 0).forGetter(t -> t.pendingSpins)
    ).apply(instance, PlayerGameInstance::new));

    private final UUID owner;
    private final AccountBalance accountBalance;
    private final Game game;
    private final List<SpinWheel> spinWheels;
    private BetMultiplier betMultiplier;
    private int pendingSpins;

    private PlayerGameInstance(UUID owner, AccountBalance accountBalance, Game game, List<SpinWheel> spinWheels, BetMultiplier betMultiplier, int pendingSpins) {
        this.owner = owner;
        this.accountBalance = accountBalance;
        this.game = game;
        this.spinWheels = spinWheels;
        this.betMultiplier = betMultiplier;
        this.pendingSpins = pendingSpins;

        for (int i = 0; i < this.spinWheels.size(); i++) {
            final int index = i;
            SpinWheel spinWheel = this.spinWheels.get(index);
            spinWheel.addSpinCompleteListener((slotMachine, amount) -> this.onSpinComplete(slotMachine, amount, index));
        }
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
                0
        );
    }

    public void tick(PhoenixSlotMachineBlockEntity slotMachine) {
        int index = this.game.getSelectedGameType() == GameType.LOW ? 0 : 3;
        for (int i = index; i < index + 3; i++) {
            SpinWheel wheel = this.spinWheels.get(i);
            wheel.update(slotMachine);
        }
    }

    public void startPlaying(PhoenixSlotMachineBlockEntity slotMachine, Player player) {
        int balanceCost = this.getCost(GameType.LOW);
        this.accountBalance.subtractBalance(balanceCost);
        if (game.getSelectedGameType() == GameType.HIGH) {
            int balanceCostMultiWin = this.getCost(GameType.HIGH);
            this.accountBalance.subtractMultiWinBalance(balanceCostMultiWin);
        }
        List<SpinWheel> spinWheels = this.getSpinWheelsForGame(this.game.getSelectedGameType());
        reloadSequences(spinWheels, this.game.getSelectedGameType(), PhoenixSlotMachineBlockEntity.getConfig(), player.getRandom());
        this.pendingSpins = spinWheels.size();
        RandomSource random = player.getRandom();
        int currentSpinDuration = 30;
        for (SpinWheel spinWheel : spinWheels) {
            currentSpinDuration += (5 + random.nextInt(15));
            spinWheel.startSpinning(currentSpinDuration);
        }
    }

    public AccountBalance getAccountBalance() {
        return accountBalance;
    }

    public boolean isSpinning() {
        return this.pendingSpins > 0;
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
        for (int i = 0; i < Math.min(this.spinWheels.size(), holder.spinWheels.size()); i++) {
            this.spinWheels.get(i).updateFrom(holder.spinWheels.get(i));
        }
        return this;
    }

    private void onSpinComplete(PhoenixSlotMachineBlockEntity slotMachine, float amount, int index) {
        if (--this.pendingSpins <= 0) {
            SlotMachineConfig config = PhoenixSlotMachineBlockEntity.getConfig();
            GameType gameType = this.game.getSelectedGameType();
            WinConfigurationConfig winConfiguration = config.getWinningConfiguration();
            List<SpinWheel> spinWheels = this.getSpinWheelsForGame(gameType);
            winConfiguration.resolveWin(gameType, spinWheels).ifPresent(winningCombination -> {
                Phoenix.LOGGER.info("Winning combination match found: {}", winningCombination);
                // TODO resolve correct balance output
                this.accountBalance.addBalance(this.betMultiplier.getValue(winningCombination.amount()));
            });
        }
        Level level = slotMachine.getLevel();
        Player player = level.getPlayerByUUID(this.owner);
        if (player != null) {
            slotMachine.updatePlayerView(player);
        }
    }

    private static void reloadSequences(List<SpinWheel> wheels, GameType gameType, SlotMachineConfig config, RandomSource random) {
        for (int i = 0; i < wheels.size(); i++) {
            SpinWheel wheel = wheels.get(i);
            List<String> sequence = config.generateSequence(random, gameType, i);
            wheel.setSequence(sequence);
        }
    }
}
