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
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

public final class PhoenixClient {

    public static final ClientPlatform PLATFORM = Services.load(ClientPlatform.class);
    private static final FontDescription DIGITAL_FONT = new FontDescription.Resource(Phoenix.identifier("digital"));

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
        minecraft.gui.setScreen(new PayoutScreen(minecraft.gui.screen(), request.pos(), request.payouts(), request.balance()));
    }

    public static void handlePhoenixScreenOpenRequest(S2C_OpenPhoenixMachineScreen request) {
        Minecraft instance = Minecraft.getInstance();
        instance.level.getBlockEntity(request.pos(), Phoenix.BLOCK_ENTITY_PHOENIX_SLOT_MACHINE.get()).ifPresent(slotMachine -> {
            ProblemReporter problemReporter = new ProblemReporter.ScopedCollector(slotMachine.problemPath(), Phoenix.LOGGER_SLF4J);
            ValueInput valueInput = TagValueInput.create(problemReporter, instance.level.registryAccess(), request.tag());
            slotMachine.loadWithComponents(valueInput);
            if (request.refreshOnly() && !(instance.gui.screen() instanceof PhoenixSlotMachineScreen)) {
                return;
            }
            instance.gui.setScreen(new PhoenixSlotMachineScreen(request.pos()));
        });
    }
}
