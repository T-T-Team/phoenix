package dev.tnt.phoenix.config;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.AccountType;
import dev.tnt.phoenix.data.GameType;
import dev.tnt.phoenix.data.component.SpinWheel;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.UpdateRestrictions;

@Config(id = Phoenix.MOD_ID)
public final class PhoenixConfig {

    @Configurable
    public SpinConfiguration lowSpinConfig = new SpinConfiguration(30, 5, 15, 0.9F);

    @Configurable
    public SpinConfiguration highSpinConfig = new SpinConfiguration(35, 5, 15, 0.9F);

    @Configurable
    @Configurable.Synchronized
    public SpinWheel.Rounding spinWheelRound = SpinWheel.Rounding.NEAREST;

    @Configurable
    @Configurable.Synchronized
    public AccountType lowGameTargetAccount = AccountType.WIN;

    @Configurable
    @Configurable.Synchronized
    public AccountType highGameTargetAccount = AccountType.WIN;

    @Configurable
    @Configurable.Synchronized
    public AccountType riskGameTargetAccount = AccountType.WIN;

    @Configurable
    @Configurable.Range(min = 2, max = 5)
    @Configurable.Gui.Slider
    @Configurable.Synchronized
    public int riskCycleDuration = 2;

    @Configurable
    @Configurable.Range(min = 1)
    public int minRiskDuration = 1;

    @Configurable
    @Configurable.Range(min = 3)
    public int additionalRiskDuration = 4;

    @Configurable
    @Configurable.Range(min = 2, max = 5)
    @Configurable.Gui.Slider
    public int riskGameWinMultiplier = 2;

    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 2.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00")
    public float riskGameWinStreakMultiplier = 0.0F;

    @Configurable
    @Configurable.Range(min = 1, max = 100)
    public int multiWinSpinPriceMultiplier = 4;

    @Configurable
    @Configurable.UpdateRestriction(UpdateRestrictions.MAIN_MENU)
    public boolean showItemInputValue = true;

    @Configurable
    public boolean generateSequenceOnEachSpin = true;

    public SpinConfiguration getSpinConfiguration(GameType type) {
        return type.isLow()
                ? this.lowSpinConfig
                : this.highSpinConfig;
    }

    public static final class SpinConfiguration {

        @Configurable
        @Configurable.Range(min = 10)
        public int minSpinDuration;

        @Configurable
        @Configurable.Range(min = 0)
        public int minAdditionalSpinDuration;

        @Configurable
        @Configurable.Range(min = 1)
        public int additionalSpinDuration;

        @Configurable
        @Configurable.Synchronized
        @Configurable.DecimalRange(min = 0.25, max = 1.0)
        @Configurable.Gui.Slider
        @Configurable.Gui.NumberFormat("0.0##")
        public float spinSpeed;

        public SpinConfiguration(int minSpinDuration, int minAdditionalSpinDuration, int additionalSpinDuration, float spinSpeed) {
            this.minSpinDuration = minSpinDuration;
            this.minAdditionalSpinDuration = minAdditionalSpinDuration;
            this.additionalSpinDuration = additionalSpinDuration;
            this.spinSpeed = spinSpeed;
        }
    }
}
