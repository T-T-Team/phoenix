package dev.tnt.phoenix.data.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tnt.phoenix.data.GameType;
import net.minecraft.server.level.ServerPlayer;

public class PlayerGameInstance {

    public static final Codec<PlayerGameInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AccountBalance.CODEC.fieldOf("account_balance").forGetter(PlayerGameInstance::getAccountBalance),
            Game.CODEC.fieldOf("active_spin").forGetter(t -> t.game),
            Codec.BOOL.optionalFieldOf("double_win", false).forGetter(PlayerGameInstance::isDoubleWins)
    ).apply(instance, PlayerGameInstance::new));

    private final AccountBalance accountBalance;
    private final Game game;
    private boolean doubleWins;

    private PlayerGameInstance(AccountBalance accountBalance, Game game, boolean doubleWins) {
        this.accountBalance = accountBalance;
        this.game = game;
        this.doubleWins = doubleWins;
    }

    public static PlayerGameInstance createForPlayer(ServerPlayer player) {
        return new PlayerGameInstance(
                AccountBalance.createDefault(),
                Game.create(),
                false
        );
    }

    public AccountBalance getAccountBalance() {
        return accountBalance;
    }

    public Game getGame() {
        return game;
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
