package dev.tnt.phoenix.client.screen;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.PhoenixSlotMachineBlock;
import dev.tnt.phoenix.block.entity.ActionType;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.client.PhoenixClient;
import dev.tnt.phoenix.client.screen.widget.*;
import dev.tnt.phoenix.data.*;
import dev.tnt.phoenix.data.game.*;
import dev.tnt.phoenix.network.C2S_SlotMachineRequest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class PhoenixSlotMachineScreen extends Screen {

    // textures
    private static final Identifier BUTTON_ADVANCED = Phoenix.identifier("textures/gui/button_advanced.png");
    private static final Identifier BUTTON_BET = Phoenix.identifier("textures/gui/button_bet.png");
    private static final Identifier BUTTON_HOLD = Phoenix.identifier("textures/gui/button_hold.png");
    private static final Identifier BUTTON_MULTIWIN = Phoenix.identifier("textures/gui/button_multiwin.png");
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
    private int leftPos;
    private int topPos;
    private boolean isLoading;

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
        PlayerGameInstance data = this.blockEntity.getPlayerData(this.minecraft.player.getUUID());
        this.isLoading = false;
        if (data == null) {
            this.isLoading = true;
            return;
        }
        AccountBalance balance = data.getAccountBalance();
        Game game = data.getGame();
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
        this.addBottomButtonRow(data);

        // top right buttons
        IconButtonWithHighlightWidget multiWin = this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + CONTENT_WIDTH - 46, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_multiwin"), BUTTON_MULTIWIN, this::onMultiWinButtonClicked));
        multiWin.active = !data.isLocked() && balance.getMultiWinBalance() > 0;

        IconButtonWithHighlightWidget payout = this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + CONTENT_WIDTH - 26, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_pay"), BUTTON_PAY, this::onPayoutButtonClicked));
        payout.active = !data.isLocked() && balance.getMultiWinBalance() > 0;

        // wheels
        WinConfigurationConfig winConfigurationConfig = config.getWinningConfiguration();
        WinConfiguration lowConfiguration = winConfigurationConfig.getConfigForGame(GameType.LOW);
        WinConfiguration highConfiguration = winConfigurationConfig.getConfigForGame(GameType.HIGH);
        this.addSpinWheels(data, config, lowConfiguration, highConfiguration);

        // win combinations - low
        GameType activeGameType = data.getGame().getSelectedGameType();
        List<WinCombination> winCombinationsDisplay = lowConfiguration.getDisplayableCombinations(true);
        WinCombinationsWidget lowWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + 10, this.topPos + CONTENT_HEIGHT - 51, CONTENT_WIDTH - 20, 30, this.font, config, winCombinationsDisplay));
        lowWinsWidget.setGrid(3, 6, 3);
        lowWinsWidget.setLayout(8, 5, 1, 3);
        lowWinsWidget.setOffsets(2, 2);
        lowWinsWidget.setTextColor(0xFFDDDD00);
        lowWinsWidget.setDisabledTextColor(0xFFAAAA00);
        lowWinsWidget.setBlankSprite(BLANK);
        lowWinsWidget.active = activeGameType == GameType.LOW && (balance.getInputBalance() > 0 || balance.getMultiWinBalance() > 0 || data.getLockReason() == LockReason.SPIN || data.getLockReason() == LockReason.RISK);

        // win combinations - special
        IconButtonWithHighlightWidget firstButton = this.holdButtons.getFirst();
        List<WinCombination> specialCombinationsDisplay = lowConfiguration.getDisplayableCombinations(false);
        WinCombinationsWidget specialWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + 10, firstButton.getY() - 79, 41, 45, this.font, config, specialCombinationsDisplay));
        specialWinsWidget.setGrid(4, 1, 3);
        specialWinsWidget.setLayout(8, 5, 3, 3);
        specialWinsWidget.setTextColor(0xFFDDDD00);
        specialWinsWidget.setDisabledTextColor(0xFFAAAA00);
        specialWinsWidget.setOffsets(2);
        specialWinsWidget.active = activeGameType == GameType.LOW && (balance.getInputBalance() > 0 || balance.getMultiWinBalance() > 0 || data.getLockReason() == LockReason.SPIN || data.getLockReason() == LockReason.RISK);

        // bet multiplier
        BalanceWidget betAmount = this.addRenderableOnly(new BalanceWidget(this.leftPos + 10, firstButton.getY() - 100, 41, 16, data::getBetMultiplierValue, this.font));
        betAmount.setDigits(3);
        betAmount.setTextColor(0xFF00FF00);
        betAmount.setTextCorrectionOffset(0.5F, 0.5F);

        // win combinations - high
        List<WinCombination> highCombinationsDisplay = highConfiguration.getDisplayableCombinations(true, Comparator.comparingInt(WinCombination::amount).reversed());
        WinCombinationsWidget highWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + CONTENT_WIDTH - 65, this.topPos + 23, 55, 120, this.font, config, highCombinationsDisplay));
        highWinsWidget.setGrid(10, 1, 3);
        highWinsWidget.setLayout(12, 7, 1, 3);
        highWinsWidget.setTextColor(0xFFDDDD00);
        highWinsWidget.setDisabledTextColor(0xFFAAAA00);
        highWinsWidget.setOffsets(2, 2);
        highWinsWidget.active = activeGameType == GameType.HIGH;

        // multi win balance
        BalanceWidget multiWinBalanceWidget = this.addRenderableOnly(new BalanceWidget(firstButton.getX() - 11, this.topPos + 115, 80, 24, balance::getMultiWinBalance, this.font));
        multiWinBalanceWidget.setTextScale(2.0F);
        multiWinBalanceWidget.setTextColor(0xFFFFFF00);
        multiWinBalanceWidget.setDigits(6);
        multiWinBalanceWidget.setTextCorrectionOffset(-16.75F, 0.25F);

        // account balance
        BalanceWidget accountBalanceWidget = this.addRenderableOnly(new BalanceWidget(this.leftPos + CONTENT_WIDTH - 70, this.topPos + CONTENT_HEIGHT - 69, 60, 16, balance::getInputBalance, this.font));
        accountBalanceWidget.setDigits(9);
        accountBalanceWidget.setTextCorrectionOffset(0.5F, 0.5F);

        // win balance
        Supplier<Integer> provider = balance::getWinBalanceForDisplay;
        BalanceWidget winBalanceWidget = this.addRenderableOnly(new BalanceWidget(this.leftPos + CONTENT_WIDTH - 62, this.topPos + CONTENT_HEIGHT - 87, 44, 16, provider, this.font));
        winBalanceWidget.setDigits(6);
        winBalanceWidget.setTextCorrectionOffset(0.5F, 0.5F);

        // risk
        RiskWidget riskWidget = this.addRenderableOnly(new RiskWidget(this.leftPos + CONTENT_WIDTH - 60, this.topPos + CONTENT_HEIGHT - 109, 40, 20, game));
        riskWidget.active = game.isRiskActive();
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

    private void addSpinWheels(PlayerGameInstance instance, SlotMachineConfig config, WinConfiguration low, WinConfiguration high) {
        GameType activeGame = instance.getGame().getSelectedGameType();
        AccountBalance account = instance.getAccountBalance();
        // wheels
        for (int i = 0; i < this.holdButtons.size(); i++) {
            IconButtonWithHighlightWidget holdButton = this.holdButtons.get(i);
            int offset = (i - 1) * 5;
            SpinWheel lowSpinWheel = instance.getSpinWheel(GameType.LOW, i);
            SpinWheelWidget lowWidget = this.addRenderableOnly(new SpinWheelWidget(holdButton.getX() - 2 + offset, holdButton.getY() - 89, holdButton.getWidth() + 4, 55, config, lowSpinWheel));
            lowWidget.active = activeGame == GameType.LOW && (account.getInputBalance() > 0 || account.getMultiWinBalance() > 0 || instance.getLockReason() == LockReason.SPIN || instance.getLockReason() == LockReason.RISK);
            lowWidget.setSpriteType(lowWidget.active ? SpriteType.DEFAULT : SpriteType.DISABLED);

            SpinWheel highSpinWheel = instance.getSpinWheel(GameType.HIGH, i);
            SpinWheelWidget highWidget = this.addRenderableOnly(new SpinWheelWidget(holdButton.getX() - 2 + offset, holdButton.getY() - 185, holdButton.getWidth() + 4, 55, config, highSpinWheel));
            highWidget.active = activeGame == GameType.HIGH;
            highWidget.setSpriteType(highWidget.active ? SpriteType.DEFAULT : SpriteType.DISABLED);
        }
    }

    private void addBottomButtonRow(PlayerGameInstance instance) {
        AccountBalance account = instance.getAccountBalance();
        Game game = instance.getGame();
        int buttonOffset = 5;
        int buttonWidth = 16 + buttonOffset;
        int rowLeft = this.leftPos + (CONTENT_WIDTH - (8 * buttonWidth - buttonOffset)) / 2;
        int rowTop = this.topPos + CONTENT_HEIGHT - 20;

        IconButtonWithHighlightWidget advancedButton = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_advanced"), BUTTON_ADVANCED, this::onAdvancedButtonClicked));
        advancedButton.active = !instance.isLocked() && ((account.getInputBalance() > 0 && account.getMultiWinBalance() >= instance.getCost(GameType.HIGH)) || game.getSelectedGameType().isHigh());

        IconButtonWithHighlightWidget betButton = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_bet"), BUTTON_BET, this::onBetButtonClicked));
        betButton.active = !instance.isLocked() && (account.getInputBalance() > 0 || account.getMultiWinBalance() > 0) && game.getHeldCount() == 0;

        for (int i = 0; i < SPIN_WHEELS; i++) {
            int posIndex = i + 2;
            final int index = i;
            IconButtonWithHighlightWidget widget = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * posIndex, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_hold"), BUTTON_HOLD, () -> this.onHoldButtonClicked(index)));
            boolean isHeld = game.getSelectedGameType() == GameType.LOW && game.isHeld(i);
            widget.active = !instance.isLocked() && game.getSelectedGameType() == GameType.LOW && !game.isHeld(i) && game.getHeldCount() < 2 && game.hasPlayed();
            widget.setLightOnDisabled(isHeld);
            this.holdButtons.add(widget);
        }

        IconButtonWithHighlightWidget riskClubs = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 5, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_risk_clubs"), BUTTON_RISK_CLUBS, this::onRiskClubsButtonClicked));
        riskClubs.active = !instance.isLocked() && account.getWinBalance() > 0;

        IconButtonWithHighlightWidget riskHearts = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 6, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_risk_hearts"), BUTTON_RISK_HEARTS, this::onRiskHeartsButtonClicked));
        riskHearts.active = !instance.isLocked() && account.getWinBalance() > 0;

        IconButtonWithHighlightWidget startButton = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 7, rowTop, 16, 16, Component.translatable("label.phoenix.ui.button_start"), BUTTON_START, this::onStartButtonClicked));
        boolean waitingForTransfer = account.getWinBalance() > 0;
        boolean canStart = !instance.isLocked() && (waitingForTransfer || account.getInputBalance() >= instance.getCost(GameType.LOW));
        if (!waitingForTransfer && game.getSelectedGameType() == GameType.HIGH) {
            canStart = canStart && account.getMultiWinBalance() >= instance.getCost(GameType.HIGH);
        }
        startButton.active = canStart;
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
}
