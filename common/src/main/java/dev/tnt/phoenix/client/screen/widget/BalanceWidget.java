package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class BalanceWidget extends AbstractWidget {

    private static final Identifier SPRITE = Phoenix.identifier("balance");
    private final Font font;
    private float textScale = 1.0F;

    public BalanceWidget(int x, int y, int width, int height, Component text, Font font) {
        super(x, y, width, height, text);
        this.font = font;
    }

    public void setTextScale(float textScale) {
        this.textScale = textScale;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.getRight() - 3 - this.font.width(this.getMessage()) * this.textScale, this.getY() + 4);
        graphics.pose().scale(this.textScale);
        graphics.text(this.font, this.getMessage(), 0, 0, 0xFF00FFFF);
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
