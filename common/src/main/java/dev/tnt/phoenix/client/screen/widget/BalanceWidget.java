package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class BalanceWidget extends AbstractWidget {

    private static final Identifier SPRITE = Phoenix.identifier("balance");

    public BalanceWidget(int x, int y, int width, int height) {
        super(x, y, width, height, CommonComponents.EMPTY);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
