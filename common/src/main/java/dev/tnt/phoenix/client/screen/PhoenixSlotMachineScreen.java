package dev.tnt.phoenix.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.api.AccountType;
import dev.tnt.phoenix.api.LockReason;
import dev.tnt.phoenix.block.PhoenixSlotMachineBlock;
import dev.tnt.phoenix.block.entity.ActionType;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.client.PhoenixClient;
import dev.tnt.phoenix.client.screen.widget.*;
import dev.tnt.phoenix.client.sound.CountSoundInstance;
import dev.tnt.phoenix.client.sound.RiskSoundInstance;
import dev.tnt.phoenix.client.sound.SpinRollSoundInstance;
import dev.tnt.phoenix.client.sound.WinSoundInstance;
import dev.tnt.phoenix.data.*;
import dev.tnt.phoenix.data.component.*;
import dev.tnt.phoenix.network.C2S_SlotMachineRequest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class PhoenixSlotMachineScreen extends Screen {

    // textures
    private static final Identifier BUTTON_ADVANCED = Phoenix.identifier("textures/gui/button_advanced.png");
    private static final Identifier BUTTON_BET = Phoenix.identifier("textures/gui/button_bet.png");
    private static final Identifier BUTTON_HOLD = Phoenix.identifier("textures/gui/button_hold.png");
    private static final Identifier BUTTON_MULTI_WIN = Phoenix.identifier("textures/gui/button_multiwin.png");
    private static final Identifier BUTTON_PAY = Phoenix.identifier("textures/gui/button_pay.png");
    private static final Identifier BUTTON_RISK_CLUBS = Phoenix.identifier("textures/gui/button_risk_clubs.png");
    private static final Identifier BUTTON_RISK_HEARTS = Phoenix.identifier("textures/gui/button_risk_hearts.png");
    private static final Identifier BUTTON_START = Phoenix.identifier("textures/gui/button_start.png");
    private static final Identifier BLANK = Phoenix.identifier("textures/spinwheel/blank.png");
    private static final Identifier BACKGROUND = Phoenix.identifier("textures/gui/phoenix_screen.png");
    // layout
    private static final int CONTENT_WIDTH = 220;
    private static final int CONTENT_HEIGHT = 256;
    private static final int SPIN_WHEELS = 3;

    private final BlockPos pos;
    private final List<IconButtonWithHighlightWidget> holdButtons = new ArrayList<>();
    private PhoenixSlotMachineBlockEntity blockEntity;
    private PlayerGameInstance gameInstance;
    private int leftPos;
    private int topPos;
    private boolean isLoading;

    private static SpinRollSoundInstance spinRollSound;
    private static WinSoundInstance winSound;
    private static RiskSoundInstance riskSound;
    private static CountSoundInstance countSound;

    public PhoenixSlotMachineScreen(BlockPos pos) {
        super(PhoenixSlotMachineBlock.NAME);
        this.pos = pos;
    }

    @Override
    protected void init() {
        BlockEntity blockEntity = this.minecraft.level.getBlockEntity(this.pos);
        if (!(blockEntity instanceof PhoenixSlotMachineBlockEntity phoenixSlotMachineBlockEntity)) {
            this.minecraft.gui.setScreen(null);
            this.minecraft.player.sendOverlayMessage(Component.literal("Invalid slot machine!").withStyle(ChatFormatting.RED));
            return;
        }
        this.blockEntity = phoenixSlotMachineBlockEntity;
        this.gameInstance = this.blockEntity.getPlayerData(this.minecraft.player.getUUID());
        this.isLoading = false;
        if (this.gameInstance == null) {
            this.isLoading = true;
            return;
        }
        AccountBalanceComponent accountBalance = this.gameInstance.getAccountBalance();
        SpinGameComponent spinGame = this.gameInstance.getSpinGame();
        this.leftPos = (this.width - CONTENT_WIDTH) / 2;
        this.topPos = (this.height - CONTENT_HEIGHT) / 2;
        this.holdButtons.clear();

        SlotMachineConfig config = Phoenix.SLOT_MACHINES.getSlotMachine(Phoenix.SLOT_MACHINE_CONFIG_PHOENIX)
                .orElse(null);
        if (config == null) {
            this.minecraft.gui.setScreen(null);
            this.minecraft.player.sendOverlayMessage(Component.literal("Invalid slot machine config!").withStyle(ChatFormatting.RED));
            return;
        }

        // bottom buttons
        this.addBottomButtonRow(this.gameInstance);

        // top right buttons
        IconButtonWithHighlightWidget multiWin = this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + CONTENT_WIDTH - 46, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_multiwin"), BUTTON_MULTI_WIN, this::onMultiWinButtonClicked));
        multiWin.active = this.gameInstance.canWithdrawMultiWin();
        multiWin.setClickSound(Phoenix.SOUND_HOLD);

        IconButtonWithHighlightWidget payout = this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + CONTENT_WIDTH - 26, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_pay"), BUTTON_PAY, this::onPayoutButtonClicked));
        payout.active = !this.gameInstance.isLocked() && accountBalance.hasBalanceInAccount(AccountType.MULTIWIN);
        payout.setClickSound(Phoenix.SOUND_HOLD);

        // wheels
        this.addSpinWheels(this.gameInstance, config);

        // win combinations - low
        WinConfigurationConfig winConfigurationConfig = config.getWinningConfiguration();
        WinConfiguration lowConfiguration = winConfigurationConfig.getConfigForGame(GameType.LOW);
        GameType activeGameType = spinGame.gameType();
        GameWinInfo winInfo = spinGame.getWinInfo();
        boolean isLowGameAvailable = activeGameType.isLow() && (accountBalance.hasBalanceInEitherAccount(1, AccountType.INPUT, AccountType.MULTIWIN) || this.gameInstance.getLockReason().isActiveGame());
        List<WinCombination> winCombinationsDisplay = lowConfiguration.getDisplayableCombinations(true);
        WinCombinationsWidget lowWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + 10, this.topPos + CONTENT_HEIGHT - 51, CONTENT_WIDTH - 20, 30, this.font, config, winCombinationsDisplay, winInfo));
        lowWinsWidget.setGrid(3, 6, 3);
        lowWinsWidget.setLayout(8, 5, 1, 3);
        lowWinsWidget.setOffsets(2, 2);
        lowWinsWidget.setTextColor(0xFFDDDD00);
        lowWinsWidget.setDisabledTextColor(0xFFAAAA00);
        lowWinsWidget.setBlankSprite(BLANK);
        lowWinsWidget.active = isLowGameAvailable;

        // win combinations - special
        IconButtonWithHighlightWidget firstButton = this.holdButtons.getFirst();
        List<WinCombination> specialCombinationsDisplay = lowConfiguration.getDisplayableCombinations(false, Comparator.comparingInt(WinCombination::orderIndex).thenComparingInt(WinCombination::amount));
        WinCombinationsWidget specialWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + 10, firstButton.getY() - 79, 41, 45, this.font, config, specialCombinationsDisplay, winInfo));
        specialWinsWidget.setGrid(4, 1, 3);
        specialWinsWidget.setLayout(8, 5, 3, 3);
        specialWinsWidget.setTextColor(0xFFDDDD00);
        specialWinsWidget.setDisabledTextColor(0xFFAAAA00);
        specialWinsWidget.setOffsets(2);
        specialWinsWidget.active = isLowGameAvailable;

        // bet multiplier
        BalanceWidget betAmount = this.addRenderableOnly(new BalanceWidget(this.leftPos + 10, firstButton.getY() - 100, 41, 16, spinGame::getBetValue, this.font));
        betAmount.setDigits(3);
        betAmount.setTextColor(0xFF00FF00);
        betAmount.setTextCorrectionOffset(0.5F, 0.5F);

        // win combinations - high
        WinConfiguration highConfiguration = winConfigurationConfig.getConfigForGame(GameType.HIGH);
        List<WinCombination> highCombinationsDisplay = highConfiguration.getDisplayableCombinations(true, Comparator.comparingInt(WinCombination::amount).reversed());
        WinCombinationsWidget highWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + CONTENT_WIDTH - 65, this.topPos + 23, 55, 120, this.font, config, highCombinationsDisplay, winInfo));
        highWinsWidget.setGrid(10, 1, 3);
        highWinsWidget.setLayout(12, 7, 1, 3);
        highWinsWidget.setTextColor(0xFFDDDD00);
        highWinsWidget.setDisabledTextColor(0xFFAAAA00);
        highWinsWidget.setOffsets(2, 2);
        highWinsWidget.active = activeGameType.isHigh();

        // multi win balance
        BalanceWidget multiWinBalanceWidget = this.addRenderableOnly(new BalanceWidget(firstButton.getX() - 11, this.topPos + 115, 80, 24, () -> accountBalance.getBalance(AccountType.MULTIWIN), this.font));
        multiWinBalanceWidget.setTextScale(2.0F);
        multiWinBalanceWidget.setTextColor(0xFFFFFF00);
        multiWinBalanceWidget.setDigits(6);
        multiWinBalanceWidget.setTextCorrectionOffset(-16.75F, 0.25F);

        // account balance
        BalanceWidget accountBalanceWidget = this.addRenderableOnly(new BalanceWidget(this.leftPos + CONTENT_WIDTH - 70, this.topPos + CONTENT_HEIGHT - 69, 60, 16, () -> accountBalance.getBalance(AccountType.INPUT), this.font));
        accountBalanceWidget.setDigits(9);
        accountBalanceWidget.setTextCorrectionOffset(0.5F, 0.5F);

        // win balance
        Supplier<Integer> provider = accountBalance::getWinBalanceForDisplay;
        BalanceWidget winBalanceWidget = this.addRenderableOnly(new BalanceWidget(this.leftPos + CONTENT_WIDTH - 62, this.topPos + CONTENT_HEIGHT - 87, 44, 16, provider, this.font));
        winBalanceWidget.setDigits(6);
        winBalanceWidget.setTextCorrectionOffset(0.5F, 0.5F);

        // risk
        RiskGameComponent riskGame = this.gameInstance.getRiskGame();
        RiskWidget riskWidget = this.addRenderableOnly(new RiskWidget(this.leftPos + CONTENT_WIDTH - 60, this.topPos + CONTENT_HEIGHT - 109, 40, 20, riskGame));
        riskWidget.active = (!this.gameInstance.isLocked() && riskGame.isActive()) || riskGame.isStopped();

        this.initiateLoopSounds(this.gameInstance);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (this.isLoading) {
            Component text = Component.literal("Loading data...");
            graphics.text(this.font, text, (this.width - this.font.width(text)) / 2, (this.height - this.font.lineHeight) / 2, 0xFFFFFFFF);
            return;
        }
        // background
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, this.leftPos + CONTENT_WIDTH, this.topPos + CONTENT_HEIGHT, 0, 1, 0, 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            this.stopSounds();
            return super.keyPressed(event); // keep default handling of esc key too
        } else if (this.gameInstance != null) {
            if (key == InputConstants.KEY_SPACE && this.gameInstance.getSpinGame().canSpinOrTransfer()) {
                this.onStartButtonClicked();
                return true;
            }
            // more interactions? Need to also play button click sound
        }
        return super.keyPressed(event);
    }

    private void addSpinWheels(PlayerGameInstance instance, SlotMachineConfig config) {
        SpinGameComponent spinGame = instance.getSpinGame();
        GameType activeGame = spinGame.gameType();
        AccountBalanceComponent account = instance.getAccountBalance();
        GameWinInfo winInfo = spinGame.getWinInfo();
        // wheels
        for (int i = 0; i < this.holdButtons.size(); i++) {
            IconButtonWithHighlightWidget holdButton = this.holdButtons.get(i);
            int offset = (i - 1) * 5;
            SpinWheel lowSpinWheel = spinGame.getWheel(GameType.LOW, i);
            SpinWheelWidget lowWidget = this.addRenderableOnly(new SpinWheelWidget(holdButton.getX() - 2 + offset, holdButton.getY() - 89, holdButton.getWidth() + 4, 55, i, config, lowSpinWheel, winInfo));
            lowWidget.active = activeGame.isLow() && (account.hasBalanceInEitherAccount(1, AccountType.INPUT, AccountType.MULTIWIN) || instance.getLockReason().is(LockReason.SPIN, LockReason.RISK));

            SpinWheel highSpinWheel = spinGame.getWheel(GameType.HIGH, i);
            SpinWheelWidget highWidget = this.addRenderableOnly(new SpinWheelWidget(holdButton.getX() - 2 + offset, holdButton.getY() - 185, holdButton.getWidth() + 4, 55, i, config, highSpinWheel, winInfo));
            highWidget.active = activeGame.isHigh();
        }
    }

    private void addBottomButtonRow(PlayerGameInstance instance) {
        SpinGameComponent spinGame = instance.getSpinGame();
        RiskGameComponent riskGame = instance.getRiskGame();
        int buttonOffset = 5;
        int buttonWidth = 16 + buttonOffset;
        int rowLeft = this.leftPos + (CONTENT_WIDTH - (8 * buttonWidth - buttonOffset)) / 2;
        int rowTop = this.topPos + CONTENT_HEIGHT - 20;

        IconButtonWithHighlightWidget advancedButton = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_advanced"), BUTTON_ADVANCED, this::onAdvancedButtonClicked));
        advancedButton.active = spinGame.canSwapGameType();
        advancedButton.setClickSound(Phoenix.SOUND_BET);
        advancedButton.setVolume(0.1F);

        IconButtonWithHighlightWidget betButton = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_bet"), BUTTON_BET, this::onBetButtonClicked));
        betButton.active = spinGame.canToggleBet();
        betButton.setClickSound(Phoenix.SOUND_BET);
        betButton.setVolume(0.1F);

        for (int i = 0; i < SPIN_WHEELS; i++) {
            int posIndex = i + 2;
            final int index = i;
            IconButtonWithHighlightWidget widget = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * posIndex, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_hold"), BUTTON_HOLD, () -> this.onHoldButtonClicked(index)));
            boolean isHeld = spinGame.gameType().isLow() && spinGame.isWheelHeld(i);
            widget.active = spinGame.canHold(i);
            widget.setLightOnDisabled(isHeld);
            widget.setClickSound(Phoenix.SOUND_HOLD);
            this.holdButtons.add(widget);
        }

        boolean riskGameAvailable = riskGame.canStart();
        IconButtonWithHighlightWidget riskClubs = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 5, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_risk_clubs"), BUTTON_RISK_CLUBS, this::onRiskClubsButtonClicked));
        riskClubs.active = riskGameAvailable;
        riskClubs.setClickSound((SoundEvent) null);

        IconButtonWithHighlightWidget riskHearts = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 6, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_risk_hearts"), BUTTON_RISK_HEARTS, this::onRiskHeartsButtonClicked));
        riskHearts.active = riskGameAvailable;
        riskHearts.setClickSound((SoundEvent) null);

        IconButtonWithHighlightWidget startButton = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 7, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_start"), BUTTON_START, this::onStartButtonClicked));
        startButton.active = instance.getSpinGame().canSpinOrTransfer();
        startButton.setClickSound((SoundEvent) null);
    }

    private void onMultiWinButtonClicked() {
        this.sendServerRequest(ActionType.MULTIWIN);
    }

    private void onPayoutButtonClicked() {
        this.sendServerRequest(ActionType.PAYOUT);
    }

    private void onAdvancedButtonClicked() {
        this.sendServerRequest(ActionType.ADVANCED);
    }

    private void onBetButtonClicked() {
        this.sendServerRequest(ActionType.BET);
    }

    private void onHoldButtonClicked(int index) {
        ActionType holdRequest = ActionType.holdActionFromIndex(index);
        this.sendServerRequest(holdRequest);
    }

    private void onRiskClubsButtonClicked() {
        this.sendServerRequest(ActionType.RISK_CLUBS);
    }

    private void onRiskHeartsButtonClicked() {
        this.sendServerRequest(ActionType.RISK_HEARTS);
    }

    private void onStartButtonClicked() {
        this.sendServerRequest(ActionType.PLAY);
    }

    private void sendServerRequest(ActionType type) {
        BlockPos position = this.blockEntity.getBlockPos();
        PhoenixClient.PLATFORM.sendPacket(new C2S_SlotMachineRequest(position, type));
    }

    private void initiateLoopSounds(PlayerGameInstance instance) {
        SoundManager manager = this.minecraft.getSoundManager();
        SpinGameComponent spinGame = instance.getSpinGame();
        RiskGameComponent riskGame = instance.getRiskGame();
        GameWinInfo winInfo = spinGame.getWinInfo();
        RandomSource random = this.minecraft.level.getRandom();
        if (SpinRollSoundInstance.canPlay(spinGame) && this.needsRestart(manager, spinRollSound)) {
            spinRollSound = new SpinRollSoundInstance(random, spinGame);
            manager.play(spinRollSound);
        }
        if (WinSoundInstance.canPlay(winInfo) && this.needsRestart(manager, winSound)) {
            winSound = new WinSoundInstance(random, winInfo);
            manager.play(winSound);
        }
        if (RiskSoundInstance.canPlay(riskGame) && this.needsRestart(manager, riskSound)) {
            riskSound = new RiskSoundInstance(random, riskGame);
            manager.play(riskSound);
        }
        if (CountSoundInstance.canPlay(instance) && this.needsRestart(manager, countSound)) {
            countSound = new CountSoundInstance(random, instance);
            manager.play(countSound);
        }
    }

    private void stopSounds() {
        SoundManager manager = this.minecraft.getSoundManager();
        if (spinRollSound != null && manager.isActive(spinRollSound)) {
            manager.stop(spinRollSound);
        }
        if (winSound != null && manager.isActive(winSound)) {
            manager.stop(winSound);
        }
        if (riskSound != null && manager.isActive(riskSound)) {
            manager.stop(riskSound);
        }
        if (countSound != null && manager.isActive(countSound)) {
            manager.stop(countSound);
        }
    }

    private boolean needsRestart(SoundManager manager, @Nullable AbstractTickableSoundInstance instance) {
        return instance == null || !manager.isActive(instance);
    }
}
