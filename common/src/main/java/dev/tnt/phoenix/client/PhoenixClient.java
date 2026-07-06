package dev.tnt.phoenix.client;

import dev.tnt.phoenix.Phoenix;
import dev.tnt.phoenix.client.platform.ClientPlatform;
import dev.tnt.phoenix.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public final class PhoenixClient {

    public static final ClientPlatform PLATFORM = Services.load(ClientPlatform.class);
    private static final FontDescription DIGITAL_FONT = new FontDescription.Resource(Phoenix.identifier("digital"));

    public static void init() {

    }

    public static MutableComponent getDigitalText(int value) {
        return Component.literal(String.valueOf(Mth.abs(value)))
                .withStyle(style -> style.withFont(DIGITAL_FONT));
    }
}
