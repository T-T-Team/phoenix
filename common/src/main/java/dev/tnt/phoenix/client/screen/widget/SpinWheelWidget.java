package dev.tnt.phoenix.client.screen.widget;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.screen.SymbolRenderHelper;
import dev.tnt.phoenix.data.GameWinInfo;
import dev.tnt.phoenix.data.MatchedWinCombination;
import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.SpriteType;
import dev.tnt.phoenix.data.component.SpinWheel;
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
    private final GameWinInfo winInfo;

    public SpinWheelWidget(int x, int y, int width, int height, int wheelIndex, SlotMachineConfig config, SpinWheel spinWheel, GameWinInfo winInfo) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.wheelIndex = wheelIndex;
        this.config = config;
        this.spinWheel = spinWheel;
        this.displayedIcons = height / (ICON_SIZE + 2) + 1;
        this.winInfo = winInfo;
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
        MatchedWinCombination winCombination = this.winInfo.isAnimatingWin() ? this.winInfo.getAnimatedWinCombination() : null;
        boolean isBlinkMode = this.winInfo.isBlinkMode();
        List<Integer> winIndexes = winCombination != null ? winCombination.pattern().indexes() : Collections.emptyList();
        List<String> sequence = this.spinWheel.getSequence();
        for (int i = startSpinIndex; i < startSpinIndex + this.displayedIcons; i++) {
            int spriteIndex = Math.floorMod(i, sequence.size());
            int positionIndex = i - startSpinIndex;
            String symbol = sequence.get(spriteIndex);
            int winningIndex = winCombination != null && this.wheelIndex < winCombination.count() ? winIndexes.get(this.wheelIndex) : -1;
            int y = this.getY() + 2 + positionIndex * (ICON_SIZE + 2);
            SymbolRenderHelper.renderSymbol(symbol, this.config, graphics, this.getX() + 2, y - spinOffset, ICON_SIZE, ICON_SIZE, this.resolveSpriteType(positionIndex, winningIndex, isBlinkMode));
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
        if (this.winInfo.isAnimateAll()) {
            return this.winInfo.isWinningIndex(this.wheelIndex, index) ? SpriteType.ENABLED : SpriteType.DISABLED;
        }
        if (this.winInfo.isAnimatingWin()) {
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
        if (this.winInfo.isAnimatingWin() || this.winInfo.isAnimateAll()) {
            return SPRITE_SLOT;
        }
        return SPRITE_SLOT_ON;
    }
}
