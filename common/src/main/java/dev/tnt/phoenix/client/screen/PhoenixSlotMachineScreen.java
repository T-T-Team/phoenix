package dev.tnt.phoenix.client.screen;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.widget.IconButtonWithHighlightWidget;
import dev.tnt.phoenix.client.screen.widget.SpinWheelWidget;
import dev.tnt.phoenix.client.screen.widget.WinCombinationsWidget;
import dev.tnt.phoenix.data.GameType;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.WinCombination;
import dev.tnt.phoenix.menu.PhoenixSlotMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class PhoenixSlotMachineScreen extends AbstractContainerScreen<PhoenixSlotMachineMenu> {

    // textures
    private static final Identifier BUTTON_ADVANCED = Phoenix.identifier("textures/gui/button_advanced.png");
    private static final Identifier BUTTON_BET = Phoenix.identifier("textures/gui/button_bet.png");
    private static final Identifier BUTTON_HOLD = Phoenix.identifier("textures/gui/button_hold.png");
    private static final Identifier BUTTON_MULTIWIN = Phoenix.identifier("textures/gui/button_multiwin.png");
    private static final Identifier BUTTON_PAY = Phoenix.identifier("textures/gui/button_pay.png");
    private static final Identifier BUTTON_RISK_CLUBS = Phoenix.identifier("textures/gui/button_risk_clubs.png");
    private static final Identifier BUTTON_RISK_HEARTS = Phoenix.identifier("textures/gui/button_risk_hearts.png");
    private static final Identifier BUTTON_START = Phoenix.identifier("textures/gui/button_start.png");
    // layout
    private static final int CONTENT_WIDTH = 220;
    private static final int CONTENT_HEIGHT = 256;
    private static final int SPIN_WHEELS = 3;

    private final List<IconButtonWithHighlightWidget> holdButtons = new ArrayList<>();

    public PhoenixSlotMachineScreen(PhoenixSlotMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    protected void init() {
        this.holdButtons.clear();
        super.init();
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
        this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + this.imageWidth - 40, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_multiwin"), BUTTON_MULTIWIN));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + this.imageWidth - 20, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_pay"), BUTTON_PAY));
        // wheels
        List<Identifier> sprites = config.getSprites();
        for (int i = 0; i < this.holdButtons.size(); i++) {
            IconButtonWithHighlightWidget holdButton = this.holdButtons.get(i);
            int offset = (i - 1) * 5;
            this.addRenderableOnly(new SpinWheelWidget(holdButton.getX() - 2 + offset, holdButton.getY() - 95, holdButton.getWidth() + 4, 55, sprites));
        }
        // win combinations
        List<WinCombination> winCombinationsDisplay = config.getWinCombinations(GameType.LOW, true);
        WinCombinationsWidget widget = this.addRenderableOnly(new WinCombinationsWidget(this.leftPos + 10, this.topPos + this.imageHeight - 57, this.imageWidth - 20, 35, this.font, config, winCombinationsDisplay));
        widget.setGrid(3, 6, 3);
        widget.setLayout(8, 5, 3, 2);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        // background
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x66404040);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    private void addBottomButtonRow(int count, int size) {
        int buttonOffset = 5;
        int buttonWidth = size + buttonOffset;
        int rowLeft = this.leftPos + (this.imageWidth - (count * buttonWidth - buttonOffset)) / 2;
        int rowTop = this.topPos + this.imageHeight - 20;
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft, rowTop, size, size, Component.translatable("label.phoenix.ui.button_advanced"), BUTTON_ADVANCED));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth, rowTop, size, size, Component.translatable("label.phoenix.ui.button_bet"), BUTTON_BET));
        for (int i = 0; i < SPIN_WHEELS; i++) {
            int posIndex = i + 2;
            IconButtonWithHighlightWidget widget = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * posIndex, rowTop, size, size, Component.translatable("label.phoenix.ui.button_hold"), BUTTON_HOLD));
            this.holdButtons.add(widget);
        }
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 5, rowTop, size, size, Component.translatable("label.phoenix.ui.button_risk_clubs"), BUTTON_RISK_CLUBS));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 6, rowTop, size, size, Component.translatable("label.phoenix.ui.button_risk_hearts"), BUTTON_RISK_HEARTS));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 7, rowTop, size, size, Component.translatable("label.phoenix.ui.button_start"), BUTTON_START));
    }

}
