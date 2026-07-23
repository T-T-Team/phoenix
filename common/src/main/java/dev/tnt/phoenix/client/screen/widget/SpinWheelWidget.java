package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.data.*;
import dev.tnt.phoenix.data.game.PlayerGameInstance;
import dev.tnt.phoenix.data.game.SpinWheel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

public final class SpinWheelWidget extends AbstractWidget {

    private static final Identifier SPRITE_SLOT = Phoenix.identifier("slot");
    private static final Identifier SPRITE_SLOT_ON = Phoenix.identifier("slot_on");
    private static final int ICON_SIZE = 16;
    private final SlotMachineConfig config;
    private final SpinWheel spinWheel;
    private final int displayedIcons;
    private final int wheelIndex;
    private SpriteType spriteType = SpriteType.DEFAULT;
    private final PlayerGameInstance instance;

    public SpinWheelWidget(int x, int y, int width, int height, int wheelIndex, SlotMachineConfig config, SpinWheel spinWheel, PlayerGameInstance instance) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.wheelIndex = wheelIndex;
        this.config = config;
        this.spinWheel = spinWheel;
        this.displayedIcons = height / (ICON_SIZE + 2) + 1;
        this.instance = instance;
    }

    public void setSpriteType(SpriteType spriteType) {
        this.spriteType = spriteType;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.resolveBackgroundSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        Minecraft instance = Minecraft.getInstance();
        DeltaTracker tracker = instance.getDeltaTracker();
        float delta = tracker.getGameTimeDeltaPartialTick(true);
        float spin = this.spinWheel.getSpinAmount(delta);
        int startSpinIndex = Mth.floor(spin);
        int spinOffset = Mth.floor((spin - startSpinIndex) * ICON_SIZE);
        GameWinInfo winInfo = this.instance.getWinInfo();
        MatchedWinCombination winCombination = winInfo.isAnimatingWin() ? winInfo.getAnimatedWinCombination() : null;
        boolean isBlinkMode = winInfo.isBlinkMode();
        List<Integer> winIndexes = winCombination != null ? winCombination.pattern().indexes() : Collections.emptyList();
        List<String> sequence = this.spinWheel.getSequence();
        for (int i = startSpinIndex; i < startSpinIndex + this.displayedIcons; i++) {
            int spriteIndex = Math.floorMod(i, sequence.size());
            int positionIndex = i - startSpinIndex;
            String symbol = sequence.get(spriteIndex);
            int winningIndex = winCombination != null && this.wheelIndex < winCombination.count() ? winIndexes.get(this.wheelIndex) : -1;
            Identifier sprite = this.config.getSprite(symbol, this.resolveSpriteType(positionIndex, winningIndex, isBlinkMode));
            int y = this.getY() + 2 + positionIndex * (ICON_SIZE + 2);
            graphics.blit(sprite, this.getX() + 2, y - spinOffset, this.getX() + 2 + ICON_SIZE, y + ICON_SIZE - spinOffset, 0.0F, 1.0F, 0.0F, 1.0F);
        }
        graphics.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    private SpriteType resolveSpriteType(int index, int winningIndex, boolean isBlinkMode) {
        if (!this.active) {
            return SpriteType.DISABLED;
        }
        GameWinInfo winInfo = this.instance.getWinInfo();
        if (winInfo.isAnimateAll()) {
            return winInfo.isWinningIndex(this.wheelIndex, index) ? SpriteType.ENABLED : SpriteType.DISABLED;
        }
        if (winInfo.isAnimatingWin()) {
            boolean highlight = true;
            if (isBlinkMode) {
                long time = System.currentTimeMillis();
                highlight = (time % 150L) < 75L;
            }
            return winningIndex == index ? (highlight ? SpriteType.ENABLED : SpriteType.DEFAULT) : SpriteType.DISABLED;
        }
        return SpriteType.DEFAULT;
    }

    private Identifier resolveBackgroundSprite() {
        if (!this.active) {
            return SPRITE_SLOT;
        }
        GameWinInfo winInfo = this.instance.getWinInfo();
        if (winInfo.isAnimatingWin() || winInfo.isAnimateAll()) {
            return SPRITE_SLOT;
        }
        return SPRITE_SLOT_ON;
    }
}
