package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.GameType;
import dev.tnt.phoenix.data.SlotMachineConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class PlayerGameInstance {

    public static final Codec<PlayerGameInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AccountBalance.CODEC.fieldOf("account_balance").forGetter(PlayerGameInstance::getAccountBalance),
            Game.CODEC.fieldOf("active_spin").forGetter(t -> t.game),
            SpinWheel.CODEC.listOf().fieldOf("spin_wheels").forGetter(t -> t.spinWheels),
            Codec.BOOL.optionalFieldOf("double_win", false).forGetter(PlayerGameInstance::isDoubleWins)
    ).apply(instance, PlayerGameInstance::new));

    private final AccountBalance accountBalance;
    private final Game game;
    private final List<SpinWheel> spinWheels;
    private boolean doubleWins;

    private PlayerGameInstance(AccountBalance accountBalance, Game game, List<SpinWheel> spinWheels, boolean doubleWins) {
        this.accountBalance = accountBalance;
        this.game = game;
        this.spinWheels = spinWheels;
        this.doubleWins = doubleWins;
    }

    public static PlayerGameInstance createForPlayer(ServerPlayer player, SlotMachineConfig config) {
        List<SpinWheel> spinWheelList = new ArrayList<>(6);
        RandomSource random = player.getRandom();
        for (int i = 0; i < 6; i++) {
            GameType type = i < 3 ? GameType.LOW : GameType.HIGH;
            int generatorIdx = i % 3;
            List<String> sequence = config.generateSequence(random, type, generatorIdx);
            SpinWheel spinWheel = new SpinWheel(sequence, 0.0F);
            spinWheelList.add(spinWheel);
        }
        return new PlayerGameInstance(
                AccountBalance.createDefault(),
                Game.create(),
                spinWheelList,
                false
        );
    }

    public AccountBalance getAccountBalance() {
        return accountBalance;
    }

    public Game getGame() {
        return game;
    }

    public SpinWheel getSpinWheel(GameType type, int index) {
        int listIndex = (type.ordinal() * 3 + index) % this.spinWheels.size();
        return this.spinWheels.get(listIndex);
    }

    public void toggleDoubleWins() {
        this.doubleWins = !this.doubleWins;
    }

    public boolean isDoubleWins() {
        return this.doubleWins;
    }

    public int getCost(GameType type) {
        int multiplier = this.isDoubleWins() ? 2 : 1;
        int baseCost = switch (type) {
            case LOW -> 1;
            case HIGH -> 4;
        };
        return baseCost * multiplier;
    }

    public PlayerGameInstance update(PlayerGameInstance holder) {
        this.accountBalance.updateFrom(holder.accountBalance);
        this.game.updateFrom(holder.game);
        this.doubleWins = holder.doubleWins;
        return this;
    }
}
