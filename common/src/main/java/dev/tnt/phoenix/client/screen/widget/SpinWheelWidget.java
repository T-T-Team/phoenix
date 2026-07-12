package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.PhoenixSlotMachineScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;

public final class SpinWheelWidget extends AbstractWidget {

    private static final Identifier SPRITE_SLOT = Phoenix.identifier("slot");
    private static final Identifier SPRITE_SLOT_ON = Phoenix.identifier("slot_on");
    private static final int ICON_SIZE = 16;
    private final List<Identifier> sprites;
    private final List<Identifier> offSprites;
    private final int displayedIcons;
    private int scroll;

    public SpinWheelWidget(int x, int y, int width, int height, List<Identifier> sprites) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.sprites = sprites;
        this.offSprites = sprites.stream()
                .map(sprite -> sprite.withPath(path -> path.replace(".png", "_off.png")))
                .toList();
        this.displayedIcons = height / (ICON_SIZE + 2) + 1;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.active ? SPRITE_SLOT_ON : SPRITE_SLOT, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        List<Identifier> activeSpriteSet = this.active ? this.sprites : this.offSprites;
        for (int i = this.scroll; i < this.scroll + this.displayedIcons; i++) {
            int index = i - this.scroll;
            int spriteIndex = i % activeSpriteSet.size();
            Identifier sprite = activeSpriteSet.get(spriteIndex);
            int y = this.getY() + 2 + index * (ICON_SIZE + 2);
            graphics.blit(sprite, this.getX() + 2, y, this.getX() + 2 + ICON_SIZE, y + ICON_SIZE, 0.0F, 1.0F, 0.0F, 1.0F);
        }
        if (this.isHovered) {
            graphics.setTooltipForNextFrame(Component.literal("Offset: " + this.scroll).withStyle(ChatFormatting.DARK_GRAY), mouseX, mouseY);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int pos = Mth.clamp(this.scroll - (int) scrollY, 0, this.sprites.size() - this.displayedIcons + 1);
        if (this.scroll != pos) {
            this.scroll = pos;
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
