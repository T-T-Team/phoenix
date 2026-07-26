package dev.tnt.phoenix.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.function.Supplier;

public final class IconButtonWithHighlightWidget extends AbstractButton {

    private final Identifier icon;
    private final Identifier iconHighlight;
    private final ClickHandler onClick;
    private Long blinkInterval = 500L;
    private boolean lightOnDisabled = false;
    private @Nullable SoundEvent clickSound;

    public IconButtonWithHighlightWidget(int x, int y, int width, int height, Component tooltip, Identifier icon, ClickHandler onClick) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.icon = icon;
        this.iconHighlight = icon.withPath(path -> path.replace(".png", "_on.png"));
        this.onClick = onClick;
        this.setTooltip(Tooltip.create(tooltip));
        this.setTooltipDelay(Duration.ofMillis(300L));
        this.clickSound = SoundEvents.UI_BUTTON_CLICK.value();
    }

    public void setBlinkInterval(Long blinkInterval) {
        this.blinkInterval = blinkInterval;
    }

    public void setLightOnDisabled(boolean lightOnDisabled) {
        this.lightOnDisabled = lightOnDisabled;
    }

    public void setClickSound(@Nullable SoundEvent clickSound) {
        this.clickSound = clickSound;
    }

    public void setClickSound(Supplier<SoundEvent> clickSound) {
        this.setClickSound(clickSound.get());
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean on = this.blinkInterval == null || (System.currentTimeMillis() % (this.blinkInterval * 2L)) <= this.blinkInterval;
        Identifier icon = (this.active && on) || (!this.active && this.lightOnDisabled) ? this.iconHighlight : this.icon;
        graphics.blit(icon, this.getX(), this.getY(), this.getRight(), this.getBottom(), 0.0F, 1.0F, 0.0F, 1.0F);
        if (this.active && this.isHovered) {
            graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x44FFFFFF);
        }
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onClick.onClick();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (this.clickSound != null) {
            soundManager.play(SimpleSoundInstance.forUI(this.clickSound, 1.0F));
        }
    }

    @FunctionalInterface
    public interface ClickHandler {
        void onClick();
    }
}
