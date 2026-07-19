package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.SpriteType;
import dev.tnt.phoenix.data.game.Game;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

public final class RiskWidget extends AbstractWidget {

    private static final Identifier SPRITE = Phoenix.identifier("balance");
    private static final Identifier RISK_CLUBS = Phoenix.identifier("textures/gui/risk_clubs.png");
    private static final Identifier RISK_HEARTS = Phoenix.identifier("textures/gui/risk_hearts.png");

    private final Game game;

    public RiskWidget(int x, int y, int width, int height, Game game) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.game = game;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean isHearts = this.game.isHearts();
        // background
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        // risk texture
        SpriteType clubsSpriteType = this.active && !isHearts ? SpriteType.ENABLED : SpriteType.DEFAULT;
        graphics.blit(clubsSpriteType.getPath(RISK_CLUBS), this.getX() + 2, this.getY() + 2, this.getX() + 18, this.getY() + 18, 0.0F, 1.0F, 0.0F, 1.0F);

        SpriteType heartsSpriteType = this.active && isHearts ? SpriteType.ENABLED : SpriteType.DEFAULT;
        graphics.blit(heartsSpriteType.getPath(RISK_HEARTS), this.getRight() - 18, this.getY() + 2, this.getRight() - 2, this.getY() + 18, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
