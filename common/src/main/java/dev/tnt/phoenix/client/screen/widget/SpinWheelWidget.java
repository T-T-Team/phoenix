package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.PhoenixSlotMachineScreen;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.SpriteType;
import dev.tnt.phoenix.data.game.SpinWheel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
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
    private final SlotMachineConfig config;
    private final SpinWheel spinWheel;
    private final int displayedIcons;
    private List<Identifier> sprites;

    public SpinWheelWidget(int x, int y, int width, int height, SlotMachineConfig config, SpinWheel spinWheel) {
        this(x, y, width, height, config, spinWheel, SpriteType.DEFAULT);
    }

    public SpinWheelWidget(int x, int y, int width, int height, SlotMachineConfig config, SpinWheel spinWheel, SpriteType spriteType) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.config = config;
        this.spinWheel = spinWheel;
        this.displayedIcons = height / (ICON_SIZE + 2) + 1;
        this.setSpriteType(spriteType);
    }

    public void setSpriteType(SpriteType spriteType) {
        this.sprites = this.config.getSprites(spriteType, this.spinWheel.getSequence());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.active ? SPRITE_SLOT_ON : SPRITE_SLOT, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        Minecraft instance = Minecraft.getInstance();
        DeltaTracker tracker = instance.getDeltaTracker();
        float delta = tracker.getGameTimeDeltaPartialTick(true);
        float spin = this.spinWheel.getSpinAmount(delta);
        int startSpinIndex = Mth.floor(spin);
        int spinOffset = Mth.floor((spin - startSpinIndex) * ICON_SIZE);
        for (int i = startSpinIndex; i < startSpinIndex + this.displayedIcons; i++) {
            int spriteIndex = i % this.sprites.size();
            Identifier sprite = this.sprites.get(spriteIndex);
            int y = this.getY() + 2 + (i - startSpinIndex) * (ICON_SIZE + 2);
            graphics.blit(sprite, this.getX() + 2, y - spinOffset, this.getX() + 2 + ICON_SIZE, y + ICON_SIZE - spinOffset, 0.0F, 1.0F, 0.0F, 1.0F);
        }
        graphics.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
