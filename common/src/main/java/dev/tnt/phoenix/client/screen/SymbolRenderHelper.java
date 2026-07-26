package dev.tnt.phoenix.client.screen;

import dev.tnt.phoenix.data.SlotMachineConfig;
import dev.tnt.phoenix.data.SpinWheelEntry;
import dev.tnt.phoenix.data.SpriteType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class SymbolRenderHelper {

    public static void renderSymbol(String symbolId, SlotMachineConfig config, GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, SpriteType type) {
        SpinWheelEntry entry = config.getSymbolData(symbolId);
        if (entry != null) {
            renderSymbol(entry, graphics, x1, y1, x2, y2, type);
        }
    }

    public static void renderSymbol(SpinWheelEntry entry, GuiGraphicsExtractor graphics, int x, int y, int width, int height, SpriteType type) {
        SpinWheelEntry.Options options = entry.getSpriteOptions();
        boolean specialRenderer = options.useDefaultTextureOnly();
        Identifier sprite = entry.getSpriteForType(specialRenderer ? SpriteType.DEFAULT : type);
        int color = specialRenderer ? getColor(type) : 0xFFFFFFFF;

        graphics.blit(
                sprite,
                x, y, x + width, y + height,
                0.0F, 1.0F, 0.0F, 1.0F
        );
    }

    private static int getColor(SpriteType originalSpriteType) {
        int strength = switch (originalSpriteType) {
            case DEFAULT -> 0xCC;
            case ENABLED -> 0xFF;
            case DISABLED -> 0x66;
        };
        return ARGB.color(0xFF, strength, strength, strength);
    }
}
