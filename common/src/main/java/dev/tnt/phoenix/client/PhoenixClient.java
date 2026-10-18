package dev.tnt.phoenix.client;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.platform.ClientPlatform;
import dev.tnt.phoenix.client.screen.PayoutScreen;
import dev.tnt.phoenix.client.screen.PhoenixSlotMachineScreen;
import dev.tnt.phoenix.network.S2C_OpenPayoutScreen;
import dev.tnt.phoenix.network.S2C_OpenPhoenixMachineScreen;
import dev.tnt.phoenix.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class PhoenixClient {

    public static final ClientPlatform PLATFORM = Services.load(ClientPlatform.class);
    private static final ResourceLocation DIGITAL_FONT = Phoenix.identifier("digital");

    public static void init() {

    }

    public static MutableComponent getDigitalText(int value) {
        return getDigitalText(String.valueOf(value));
    }

    public static MutableComponent getDigitalText(String value) {
        return Component.literal(value)
                .withStyle(style -> style.withFont(DIGITAL_FONT));
    }

    public static void handlePayoutScreenOpenRequest(S2C_OpenPayoutScreen request) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new PayoutScreen(minecraft.screen, request.pos(), request.payouts(), request.balance()));
    }

    public static void handlePhoenixScreenOpenRequest(S2C_OpenPhoenixMachineScreen request) {
        Minecraft instance = Minecraft.getInstance();
        instance.level.getBlockEntity(request.pos(), Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get()).ifPresent(slotMachine -> {
            slotMachine.loadWithComponents(request.tag(), instance.level.registryAccess());
            if (request.refreshOnly() && !(instance.screen instanceof PhoenixSlotMachineScreen)) {
                return;
            }
            instance.setScreen(new PhoenixSlotMachineScreen(request.pos()));
        });
    }
}
