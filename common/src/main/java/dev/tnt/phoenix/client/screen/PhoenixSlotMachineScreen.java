package dev.tnt.phoenix.client.screen;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.block.PhoenixSlotMachineBlock;
import dev.tnt.phoenix.block.entity.PhoenixSlotMachineBlockEntity;
import dev.tnt.phoenix.client.PhoenixClient;
import dev.tnt.phoenix.client.screen.widget.*;
import dev.tnt.phoenix.data.*;
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
    // layout
    private static final int CONTENT_WIDTH = 220;
    private static final int CONTENT_HEIGHT = 256;
    private static final int SPIN_WHEELS = 3;

    private final BlockPos pos;
    private final List<IconButtonWithHighlightWidget> holdButtons = new ArrayList<>();
    private PhoenixSlotMachineBlockEntity blockEntity;
    private int leftPos;
    private int topPos;

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
        DataInstanceHolder data = this.blockEntity.getPlayerData(this.minecraft.player.getUUID());
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
        this.addBottomButtonRow(8, 16);

        // top right buttons
        this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + CONTENT_WIDTH - 46, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_multiwin"), BUTTON_MULTIWIN, this::onMultiWinButtonClicked));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + CONTENT_WIDTH - 26, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_pay"), BUTTON_PAY, this::onPayoutButtonClicked));

        // wheels
        WinConfiguration lowConfiguration = config.getWinningConfiguration(GameType.LOW);
        WinConfiguration highConfiguration = config.getWinningConfiguration(GameType.HIGH);
        this.addSpinWheels(config, lowConfiguration, highConfiguration);

        // win combinations - low
        List<WinCombination> winCombinationsDisplay = lowConfiguration.getDisplayableCombinations(true);
        WinCombinationsWidget lowWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + 10, this.topPos + CONTENT_HEIGHT - 51, CONTENT_WIDTH - 20, 30, this.font, config, winCombinationsDisplay));
        lowWinsWidget.setGrid(3, 6, 3);
        lowWinsWidget.setLayout(8, 5, 1, 3);
        lowWinsWidget.setOffsets(2, 2);
        lowWinsWidget.setTextColor(0xFFCCCC00);
        lowWinsWidget.setBlankSprite(BLANK);

        // win combinations - special
        IconButtonWithHighlightWidget firstButton = this.holdButtons.getFirst();
        List<WinCombination> specialCombinationsDisplay = lowConfiguration.getDisplayableCombinations(false);
        WinCombinationsWidget specialWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + 10, firstButton.getY() - 79, 41, 45, this.font, config, specialCombinationsDisplay));
        specialWinsWidget.setGrid(4, 1, 3);
        specialWinsWidget.setLayout(8, 5, 3, 3);
        specialWinsWidget.setTextColor(0xFFCCCC00);
        specialWinsWidget.setOffsets(2);

        // win combinations - high
        List<WinCombination> highCombinationsDisplay = highConfiguration.getDisplayableCombinations(true, Comparator.comparingInt(WinCombination::amount).reversed());
        WinCombinationsWidget highWinsWidget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + CONTENT_WIDTH - 65, this.topPos + 23, 55, 120, this.font, config, highCombinationsDisplay));
        highWinsWidget.setGrid(10, 1, 3);
        highWinsWidget.setLayout(12, 7, 1, 3);
        highWinsWidget.setTextColor(0xFFCCCC00);
        highWinsWidget.setOffsets(2, 2);

        // multi win balance
        BalanceWidget multiWinBalanceWidget = this.addRenderableOnly(new BalanceWidget(firstButton.getX() - 11, this.topPos + 95, 80, 24, () -> 301500, this.font));
        multiWinBalanceWidget.setTextScale(2.0F);
        multiWinBalanceWidget.setTextColor(0xFFFFFF00);
        multiWinBalanceWidget.setDigits(6);
        multiWinBalanceWidget.setTextCorrectionOffset(-16.75F, 0.25F);

        // account balance
        BalanceWidget accountBalanceWidget = this.addRenderableOnly(new BalanceWidget(this.leftPos + CONTENT_WIDTH - 70, this.topPos + CONTENT_HEIGHT - 69, 60, 16, data::getValue, this.font));
        accountBalanceWidget.setDigits(9);
        accountBalanceWidget.setTextCorrectionOffset(0.5F, 0.5F);

        // risk balance?
        BalanceWidget riskBalanceWidget = this.addRenderableOnly(new BalanceWidget(this.leftPos + CONTENT_WIDTH - 62, this.topPos + CONTENT_HEIGHT - 87, 44, 16, () -> 2500, this.font));
        riskBalanceWidget.setDigits(6);
        riskBalanceWidget.setTextCorrectionOffset(0.5F, 0.5F);

        // risk
        this.addRenderableOnly(new RiskWidget(this.leftPos + CONTENT_WIDTH - 60, this.topPos + CONTENT_HEIGHT - 109, 40, 20));

    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        // background
        graphics.fill(this.leftPos, this.topPos, this.leftPos + CONTENT_WIDTH, this.topPos + CONTENT_HEIGHT, 0x66404040);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addSpinWheels(SlotMachineConfig config, WinConfiguration low, WinConfiguration high) {
        IconButtonWithHighlightWidget first = this.holdButtons.getFirst();
        IconButtonWithHighlightWidget last = this.holdButtons.getLast();
        int leftPositionX = first.getX() - 4;
        int leftPositionY = first.getY() - 4;
        int rightPositionX = last.getX() + last.getWidth() + 4;
        int rightPositionY = last.getY() + last.getHeight() + 4;
        List<WinConfiguration.WinPattern> patterns = low.patterns();
        for (WinConfiguration.WinPattern pattern : patterns) {
            float yLeft = pattern.getLeftHeight();
            float yRight = pattern.getRightHeight();
            // TODO - either lines or custom texture layer
        }
        // wheels
        for (int i = 0; i < this.holdButtons.size(); i++) {
            IconButtonWithHighlightWidget holdButton = this.holdButtons.get(i);
            int offset = (i - 1) * 5;
            // TODO add renderable only
            List<Identifier> sequenceSprites = config.generateSequence(this.minecraft.player.getRandom(), GameType.LOW, i);
            this.addRenderableWidget(new SpinWheelWidget(holdButton.getX() - 2 + offset, holdButton.getY() - 89, holdButton.getWidth() + 4, 55, sequenceSprites));

            List<Identifier> highSequenceSprites = config.generateSequence(this.minecraft.player.getRandom(), GameType.HIGH, i);
            this.addRenderableWidget(new SpinWheelWidget(holdButton.getX() - 2 + offset, holdButton.getY() - 200, holdButton.getWidth() + 4, 55, highSequenceSprites));
        }
    }

    private void addBottomButtonRow(int count, int size) {
        int buttonOffset = 5;
        int buttonWidth = size + buttonOffset;
        int rowLeft = this.leftPos + (CONTENT_WIDTH - (count * buttonWidth - buttonOffset)) / 2;
        int rowTop = this.topPos + CONTENT_HEIGHT - 20;
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft, rowTop, size, size, Component.translatable("label.phoenix.ui.button_advanced"), BUTTON_ADVANCED, this::onAdvancedButtonClicked));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth, rowTop, size, size, Component.translatable("label.phoenix.ui.button_bet"), BUTTON_BET, this::onBetButtonClicked));
        for (int i = 0; i < SPIN_WHEELS; i++) {
            int posIndex = i + 2;
            final int index = i;
            IconButtonWithHighlightWidget widget = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * posIndex, rowTop, size, size, Component.translatable("label.phoenix.ui.button_hold"), BUTTON_HOLD, () -> this.onHoldButtonClicked(index)));
            this.holdButtons.add(widget);
        }
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 5, rowTop, size, size, Component.translatable("label.phoenix.ui.button_risk_clubs"), BUTTON_RISK_CLUBS, this::onRiskClubsButtonClicked));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 6, rowTop, size, size, Component.translatable("label.phoenix.ui.button_risk_hearts"), BUTTON_RISK_HEARTS, this::onRiskHeartsButtonClicked));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 7, rowTop, size, size, Component.translatable("label.phoenix.ui.button_start"), BUTTON_START, this::onStartButtonClicked));
    }

    private void onMultiWinButtonClicked() {
        this.sendServerRequest(C2S_SlotMachineRequest.RequestType.MULTIWIN);
    }

    private void onPayoutButtonClicked() {
        this.sendServerRequest(C2S_SlotMachineRequest.RequestType.PAYOUT);
    }

    private void onAdvancedButtonClicked() {
        this.sendServerRequest(C2S_SlotMachineRequest.RequestType.ADVANCED);
    }

    private void onBetButtonClicked() {
        this.sendServerRequest(C2S_SlotMachineRequest.RequestType.BET);
    }

    private void onHoldButtonClicked(int index) {
        C2S_SlotMachineRequest.RequestType holdRequest = C2S_SlotMachineRequest.RequestType.holdActionFromIndex(index);
        this.sendServerRequest(holdRequest);
    }

    private void onRiskClubsButtonClicked() {
        this.sendServerRequest(C2S_SlotMachineRequest.RequestType.RISK_CLUBS);
    }

    private void onRiskHeartsButtonClicked() {
        this.sendServerRequest(C2S_SlotMachineRequest.RequestType.RISK_HEARTS);
    }

    private void onStartButtonClicked() {
        this.sendServerRequest(C2S_SlotMachineRequest.RequestType.PLAY);
    }

    private void sendServerRequest(C2S_SlotMachineRequest.RequestType type) {
        BlockPos position = this.blockEntity.getBlockPos();
        PhoenixClient.PLATFORM.sendPacket(new C2S_SlotMachineRequest(position, type));
    }
}
