package dev.tnt.phoenix.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.time.Duration;

public final class IconButtonWithHighlightWidget extends AbstractButton {

    private final Identifier icon;
    private final Identifier iconHighlight;
    private final ClickHandler onClick;

    public IconButtonWithHighlightWidget(int x, int y, int width, int height, Component tooltip, Identifier icon, ClickHandler onClick) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.icon = icon;
        this.iconHighlight = icon.withPath(path -> path.replace(".png", "_on.png"));
        this.onClick = onClick;
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

    @FunctionalInterface
    public interface ClickHandler {
        void onClick();
    }
}
