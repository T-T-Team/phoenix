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
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix3x2fStack;

import java.util.function.Supplier;

public final class BalanceWidget extends AbstractWidget {

    private static final Identifier SPRITE = Phoenix.identifier("balance");
    private final Supplier<Integer> valueProvider;
    private final Font font;
    private int digits = 9;
    private int textColor = 0xFF00FFFF;
    private float textScale = 1.0F;
    private float xTextCorrection = 0.0F;
    private float yTextCorrection = 0.0F;

    public BalanceWidget(int x, int y, int width, int height, Supplier<Integer> valueProvider, Font font) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.valueProvider = valueProvider;
        this.font = font;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setTextScale(float textScale) {
        this.textScale = textScale;
    }

    public void setTextCorrectionOffset(float x, float y) {
        this.xTextCorrection = x;
        this.yTextCorrection = y;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // background
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        // text
        Integer value = this.valueProvider.get();
        String initialValue = value != null ? String.valueOf(value) : "";
        String formattedValue = StringUtils.leftPad(initialValue, this.digits, 'X').substring(0, this.digits);
        Component text = PhoenixClient.getDigitalText(formattedValue);
        int textWidth = this.font.width(text);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(this.getX() + (this.getWidth() - textWidth) / 2.0F + this.xTextCorrection, this.getY() + 3 + this.yTextCorrection);
        pose.scale(this.textScale);
        graphics.text(this.font, text, 0, 0, this.textColor, false);
        pose.popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
