package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.SlotMachineConfig;
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
    private int columnSpacing = 4;
    private int offset;

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

    public void setLayout(int iconSize, int iconOverlaySpacing, int columnSpacing, int offset) {
        this.iconSize = iconSize;
        this.iconOverlaySpacing = iconOverlaySpacing;
        this.columnSpacing = columnSpacing;
        this.offset = offset;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_PRICE_SLOT, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        for (int x = 0; x < this.columns; x++) {
            for (int y = 0; y < this.rows; y++) {
                // TODO spacing adjusted to screen size
                int px = this.getX() + this.offset + x * (this.maxColumnSize * (this.iconSize + this.columnSpacing));
                int py = this.getY() + this.offset + y * (this.maxColumnSize + this.iconSize);
                int index = y * this.columns + x;
                if (index >= this.combinations.size()) {
                    break;
                }
                WinCombination combination = this.combinations.get(index);
                Identifier icon = combination.getSprite(this.config);
                for (int i = 0; i < Math.min(this.maxColumnSize, combination.count()); i++) {
                    int left = px + i * this.iconOverlaySpacing;
                    graphics.blit(icon, left, py, left + this.iconSize, py + this.iconSize, 0.0F, 1.0F, 0.0F, 1.0F);
                }
                Component amountLabel = Component.literal(String.valueOf(combination.amount()));
                graphics.text(this.font, amountLabel, px + this.maxColumnSize * this.iconSize - 6, py, 0xFFFFFFFF, true);
            }
        }
        graphics.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
