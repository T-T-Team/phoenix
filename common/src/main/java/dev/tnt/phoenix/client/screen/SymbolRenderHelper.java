package dev.tnt.phoenix.client.screen;

import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.SpinWheelEntry;
import dev.tnt.phoenix.data.SpriteType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class SymbolRenderHelper {

    public static void renderSymbol(String symbolId, SlotMachineConfig config, GuiGraphics graphics, int x1, int y1, int x2, int y2, SpriteType type) {
        SpinWheelEntry entry = config.getSymbolData(symbolId);
        if (entry != null) {
            renderSymbol(entry, graphics, x1, y1, x2, y2, type);
        }
    }

    public static void renderSymbol(SpinWheelEntry entry, GuiGraphics graphics, int x, int y, int width, int height, SpriteType type) {
        SpinWheelEntry.Options options = entry.getSpriteOptions();
        boolean specialRenderer = options.useDefaultTextureOnly();
        ResourceLocation sprite = entry.getSpriteForType(specialRenderer ? SpriteType.DEFAULT : type);
        graphics.blit(
                sprite,
                x, y, 0,
                0.0F, 0.0F,
                width, height,
                width, height
        );
    }

    private static int getColor(SpriteType originalSpriteType) {
        int strength = switch (originalSpriteType) {
            case DEFAULT -> 0xCC;
            case ENABLED -> 0xFF;
            case DISABLED -> 0x66;
        };
        return (0xFF << 24) | (strength << 16) | (strength << 8) | strength;
    }
}
