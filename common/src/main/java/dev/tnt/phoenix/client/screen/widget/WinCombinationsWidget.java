package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.SpriteType;
import dev.tnt.phoenix.data.WinCombination;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class WinCombinationsWidget extends AbstractWidget {

    private static final Identifier SPRITE_PRICE_SLOT = Phoenix.identifier("price_slot");

    private final Font font;
    private final SlotMachineConfig config;
    private final List<WinCombination> combinations;
    private int rows;
    private int columns;
    private int maxColumnSize = 3;
    private int iconSize = 8;
    private int iconOverlaySpacing = 4;
    private int rowSpacing;
    private int columnSpacing = 4;
    private int horizontalOffset;
    private int verticalOffset;
    private int textColor = 0xFFFFFFFF;
    private int textColorDisabled = 0xFF808080;
    private Identifier blankSprite;

    public WinCombinationsWidget(int x, int y, int width, int height, Font font, SlotMachineConfig config, List<WinCombination> combinations) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.font = font;
        this.config = config;
        this.combinations = combinations;
    }

    public void setGrid(int rows, int columns, int maxColumnSize) {
        this.rows = rows;
        this.columns = columns;
        this.maxColumnSize = maxColumnSize;
    }

    public void setLayout(int iconSize, int iconOverlaySpacing, int rowSpacing, int columnSpacing) {
        this.iconSize = iconSize;
        this.iconOverlaySpacing = iconOverlaySpacing;
        this.rowSpacing = rowSpacing;
        this.columnSpacing = columnSpacing;
    }

    public void setOffsets(int horizontal, int vertical) {
        this.horizontalOffset = horizontal;
        this.verticalOffset = vertical;
    }

    public void setOffsets(int offset) {
        this.setOffsets(offset, offset);
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setDisabledTextColor(int textColor) {
        this.textColorDisabled = textColor;
    }

    public void setBlankSprite(Identifier sprite) {
        this.blankSprite = sprite;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_PRICE_SLOT, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        for (int x = 0; x < this.columns; x++) {
            for (int y = 0; y < this.rows; y++) {
                int index = x * this.rows + y;
                int px = this.getX() + this.horizontalOffset + x * (this.maxColumnSize * (this.iconSize + this.columnSpacing));
                int py = this.getY() + this.verticalOffset + y * (this.iconSize + this.rowSpacing);
                if (index >= this.combinations.size()) {
                    break;
                }
                WinCombination combination = this.combinations.get(index);
                Identifier icon = combination.getSprite(this.config, this.active ? SpriteType.DEFAULT : SpriteType.DISABLED); // TODO logic
                for (int i = 0; i < this.maxColumnSize; i++) {
                    Identifier sprite = i >= combination.count() ? this.blankSprite : icon;
                    if (sprite == null) {
                        break;
                    }
                    int left = px + i * this.iconOverlaySpacing;
                    graphics.blit(sprite, left, py, left + this.iconSize, py + this.iconSize, 0.0F, 1.0F, 0.0F, 1.0F);
                }
                Component amountLabel = Component.literal(String.valueOf(combination.amount()));
                graphics.text(this.font, amountLabel, px + this.maxColumnSize * this.iconSize - 5, py + (this.iconSize - this.font.lineHeight) / 2, this.active ? this.textColor : this.textColorDisabled, true);
            }
        }
        graphics.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
