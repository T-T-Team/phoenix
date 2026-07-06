package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.PhoenixClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.function.IntSupplier;

public final class BalanceWidget extends AbstractWidget {

    private static final Identifier SPRITE = Phoenix.identifier("balance");
    private final @Nullable IntSupplier valueProvider;
    private final Font font;
    private int textColor = 0xFF00FFFF;
    private float textScale = 1.0F;

    public BalanceWidget(int x, int y, int width, int height, @Nullable IntSupplier valueProvider, Font font) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.valueProvider = valueProvider;
        this.font = font;
    }

    public BalanceWidget(int x, int y, int width, int height) {
        this(x, y, width, height, null, null);
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setTextScale(float textScale) {
        this.textScale = textScale;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // background
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        // text
        if (this.valueProvider != null) {
            int value = this.valueProvider.getAsInt();
            Component text = PhoenixClient.getDigitalText(value);
            int textWidth = this.font.width(text);
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(this.getRight() - 3 - textWidth * this.textScale, this.getY() + 3);
            pose.scale(this.textScale);
            graphics.text(this.font, text, 0, 0, this.textColor, false);
            pose.popMatrix();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
