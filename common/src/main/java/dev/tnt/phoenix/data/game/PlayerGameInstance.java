package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.GameType;

public class PlayerGameInstance {

    public static final Codec<PlayerGameInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AccountBalance.CODEC.fieldOf("account_balance").forGetter(PlayerGameInstance::getAccountBalance),
            ActiveSpin.CODEC.fieldOf("active_spin").forGetter(t -> t.spin),
            GameType.CODEC.optionalFieldOf("selected_game_type", GameType.LOW).forGetter(PlayerGameInstance::getActiveGameType),
            Codec.BOOL.optionalFieldOf("double_win", false).forGetter(PlayerGameInstance::isDoubleWins)
    ).apply(instance, PlayerGameInstance::new));

    private final AccountBalance accountBalance;
    private final ActiveSpin spin;
    private GameType selectedGameType;
    private boolean doubleWins;

    private PlayerGameInstance(AccountBalance accountBalance, ActiveSpin spin, GameType selectedGameType, boolean doubleWins) {
        this.accountBalance = accountBalance;
        this.spin = spin;
        this.selectedGameType = selectedGameType;
        this.doubleWins = doubleWins;
    }

    public static PlayerGameInstance createDefault() {
        return new PlayerGameInstance(
                AccountBalance.createDefault(),
                new ActiveSpin(),
                GameType.LOW,
                false
        );
    }

    public AccountBalance getAccountBalance() {
        return accountBalance;
    }

    public GameType getActiveGameType() {
        return this.selectedGameType;
    }

    public boolean isPlaying() {
        return this.getActiveGameType() != null;
    }

    public boolean canPlay() {
        return true; // TODO implement
    }

    public void toggleDoubleWins() {
        this.doubleWins = !this.doubleWins;
    }

    public boolean isDoubleWins() {
        return this.doubleWins;
    }

    public void play() {

    }

    public PlayerGameInstance update(PlayerGameInstance holder) {
        this.accountBalance.updateFrom(holder.accountBalance);
        this.spin.updateFrom(holder.spin);
        this.selectedGameType = holder.selectedGameType;
        this.doubleWins = holder.doubleWins;
        return this;
    }
}
