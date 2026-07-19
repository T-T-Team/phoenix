package dev.tnt.phoenix.config;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.game.BalanceType;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;

@Config(id = Phoenix.MOD_ID)
public final class PhoenixConfig {

    @Configurable
    public SpinConfiguration lowSpinConfig = new SpinConfiguration(30, 5, 15, 0.8F);

    @Configurable
    public SpinConfiguration highSpinConfig = new SpinConfiguration(30, 5, 15, 0.8F);

    @Configurable
    @Configurable.Synchronized
    public BalanceType lowGameTargetAccount = BalanceType.WIN;

    @Configurable
    @Configurable.Synchronized
    public BalanceType highGameTargetAccount = BalanceType.WIN;

    @Configurable
    @Configurable.Synchronized
    public BalanceType riskGameTargetAccount = BalanceType.WIN;

    @Configurable
    @Configurable.Range(min = 2, max = 10)
    @Configurable.Gui.Slider
    @Configurable.Synchronized
    public int riskCycleDuration = 3;

    @Configurable
    @Configurable.Range(min = 5)
    public int minRiskDuration = 5;

    @Configurable
    @Configurable.Range(min = 1)
    public int additionalRiskDuration = 40;

    @Configurable
    @Configurable.Range(min = 2, max = 5)
    @Configurable.Gui.Slider
    public int riskGameWinMultiplier = 2;

    @Configurable
    @Configurable.Range(min = 1, max = 100)
    public int multiWinSpinPriceMultiplier = 4;

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
