package dev.tnt.phoenix.client.screen.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.PhoenixClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

public final class BalanceWidget extends AbstractWidget {

    private static final ResourceLocation SPRITE = Phoenix.identifier("balance");
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
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        // background
        graphics.blitSprite(SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        // text
        Integer value = this.valueProvider.get();
        String initialValue = value != null ? String.valueOf(value) : "";
        String formattedValue = StringUtils.leftPad(initialValue, this.digits, 'X').substring(0, this.digits);
        Component text = PhoenixClient.getDigitalText(formattedValue);
        int textWidth = this.font.width(text);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.getX() + (this.getWidth() - textWidth) / 2.0F + this.xTextCorrection, this.getY() + 3 + this.yTextCorrection, 0);
        pose.scale(this.textScale, this.textScale, this.textScale);
        graphics.drawString(this.font, text, 0, 0, this.textColor, false);
        pose.popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
