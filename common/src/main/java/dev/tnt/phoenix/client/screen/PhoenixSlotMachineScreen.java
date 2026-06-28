package dev.tnt.phoenix.client.screen;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.menu.PhoenixSlotMachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PhoenixSlotMachineScreen extends AbstractContainerScreen<PhoenixSlotMachineMenu> {

    // sprites
    private static final Identifier SPRITE_PRICE_SLOT = Phoenix.identifier("price_slot");
    private static final Identifier SPRITE_SLOT = Phoenix.identifier("slot");
    // textures
    private static final Identifier BUTTON_ADVANCED = Phoenix.identifier("textures/gui/button_advanced.png");
    private static final Identifier BUTTON_BET = Phoenix.identifier("textures/gui/button_bet.png");
    private static final Identifier BUTTON_HOLD = Phoenix.identifier("textures/gui/button_hold.png");
    private static final Identifier BUTTON_MULTIWIN = Phoenix.identifier("textures/gui/button_multiwin.png");
    private static final Identifier BUTTON_PAY = Phoenix.identifier("textures/gui/button_pay.png");
    private static final Identifier BUTTON_RISK_CLUBS = Phoenix.identifier("textures/gui/button_risk_clubs.png");
    private static final Identifier BUTTON_RISK_HEARTS = Phoenix.identifier("textures/gui/button_risk_hearts.png");
    private static final Identifier BUTTON_START = Phoenix.identifier("textures/gui/button_start.png");
    // symbols
    private static final Identifier CHERRY = Phoenix.identifier("textures/spinwheel/cherry.png");
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

        this.addBottomButtonRow(8, 16);
        this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + this.imageWidth - 40, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_multiwin"), BUTTON_MULTIWIN));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(this.leftPos + this.imageWidth - 20, this.topPos + 4, 16, 16, Component.translatable("label.phoenix.ui.button_pay"), BUTTON_PAY));

        SlotMachineConfig config = Phoenix.SLOT_MACHINES.getSlotMachine(Phoenix.SLOT_MACHINE_CONFIG_PHOENIX).orElseThrow();
        List<Identifier> sprites = config.getSprites();
        for (IconButtonWithHighlightWidget holdButton : this.holdButtons) {
            this.addRenderableWidget(new SpinWheelWidget(holdButton.getX() - 2, holdButton.getY() - 100, holdButton.getWidth() + 4, 55, sprites));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        // background TODO
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x66404040);
        int winFrameX = this.leftPos + 3;
        int winFrameY = this.topPos + this.imageHeight - 60;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_PRICE_SLOT, winFrameX, winFrameY, 120, 30);

        int rows = 3;
        int columns = 6;
        int columnSize = 3;
        int iconSize = 8;
        int iconOverlaySpacing = iconSize / 2 + 1;
        int offset = 2;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int px = winFrameX + offset + x * (columnSize * iconSize);
                int py = winFrameY + offset + y * (columnSize + iconSize);
                for (int i = 0; i < columnSize; i++) {
                    int left = px + i * iconOverlaySpacing;
                    graphics.blit(CHERRY, left, py, left + iconSize, py + iconSize, 0.0F, 1.0F, 0.0F, 1.0F);
                }
            }
        }
        graphics.blit(CHERRY, winFrameX + 2, winFrameY + 2, winFrameX + 10, winFrameY + 10, 0.0f, 1.0f, 0.0f, 1.0f);
        graphics.blit(CHERRY, winFrameX + 7, winFrameY + 2, winFrameX + 15, winFrameY + 10, 0.0f, 1.0f, 0.0f, 1.0f);
        // sprites
        // TODO

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
        for (int i = 0; i < SPIN_WHEELS; i++) {
            int posIndex = i + 1;
            IconButtonWithHighlightWidget widget = this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * posIndex, rowTop, size, size, Component.translatable("label.phoenix.ui.button_hold"), BUTTON_HOLD));
            this.holdButtons.add(widget);
        }
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 4, rowTop, size, size, Component.translatable("label.phoenix.ui.button_bet"), BUTTON_BET));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 5, rowTop, size, size, Component.translatable("label.phoenix.ui.button_risk_clubs"), BUTTON_RISK_CLUBS));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 6, rowTop, size, size, Component.translatable("label.phoenix.ui.button_risk_hearts"), BUTTON_RISK_HEARTS));
        this.addRenderableWidget(new IconButtonWithHighlightWidget(rowLeft + buttonWidth * 7, rowTop, size, size, Component.translatable("label.phoenix.ui.button_start"), BUTTON_START));
    }

    private static final class IconButtonWithHighlightWidget extends AbstractButton {

        private final Identifier icon;
        private final Identifier iconHighlight;

        public IconButtonWithHighlightWidget(int x, int y, int width, int height, Component tooltip, Identifier icon) {
            super(x, y, width, height, CommonComponents.EMPTY);
            this.icon = icon;
            this.iconHighlight = icon.withPath(path -> path.replace(".png", "_on.png"));
            this.setTooltip(Tooltip.create(tooltip));
            this.setTooltipDelay(Duration.ofMillis(300L));
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Identifier icon = this.active ? this.iconHighlight : this.icon;
            graphics.blit(icon, this.getX(), this.getY(), this.getRight(), this.getBottom(), 0.0F, 1.0F, 0.0F, 1.0F);
            if (this.active && this.isHovered) {
                graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x44FFFFFF);
            }
        }

        @Override
        public void onPress(InputWithModifiers input) {
            // TODO implement
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }

    private static final class SpinWheelWidget extends AbstractWidget {

        private static final int ICON_SIZE = 16;
        private final List<Identifier> sprites;
        private final int displayedIcons;

        public SpinWheelWidget(int x, int y, int width, int height, List<Identifier> sprites) {
            super(x, y, width, height, CommonComponents.EMPTY);
            this.sprites = sprites;
            this.displayedIcons = height / (ICON_SIZE + 2) + 1;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_SLOT, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
            for (int i = 0; i < this.displayedIcons; i++) {
                int spriteIndex = i % this.sprites.size();
                Identifier sprite = this.sprites.get(spriteIndex);
                int y = this.getY() + 2 + i * (ICON_SIZE + 2);
                graphics.blit(sprite, this.getX() + 2, y, this.getX() + 2 + ICON_SIZE, y + ICON_SIZE, 0.0F, 1.0F, 0.0F, 1.0F);
            }
            graphics.disableScissor();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
