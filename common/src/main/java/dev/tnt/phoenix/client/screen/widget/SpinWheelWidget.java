package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.PhoenixSlotMachineScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class SpinWheelWidget extends AbstractWidget {

    private static final Identifier SPRITE_SLOT = Phoenix.identifier("slot");
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
