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
            BetMultiplier.CODEC.optionalFieldOf("bet_multiplier", BetMultiplier.X1).forGetter(t -> t.betMultiplier)
    ).apply(instance, PlayerGameInstance::new));

    private final AccountBalance accountBalance;
    private final Game game;
    private final List<SpinWheel> spinWheels;
    private BetMultiplier betMultiplier;

    private PlayerGameInstance(AccountBalance accountBalance, Game game, List<SpinWheel> spinWheels, BetMultiplier betMultiplier) {
        this.accountBalance = accountBalance;
        this.game = game;
        this.spinWheels = spinWheels;
        this.betMultiplier = betMultiplier;
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
        return new PlayerGameInstance(
                AccountBalance.createDefault(),
                Game.create(),
                spinWheelList,
                BetMultiplier.X1
        );
    }

    public void tick() {
        int index = this.game.getSelectedGameType() == GameType.LOW ? 0 : 3;
        for (int i = index; i < index + 3; i++) {
            SpinWheel wheel = this.spinWheels.get(i);
            wheel.update();
        }
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
        for (int i = 0; i < Math.min(this.spinWheels.size(), holder.spinWheels.size()); i++) {
            this.spinWheels.get(i).updateFrom(holder.spinWheels.get(i));
        }
        return this;
    }
}
