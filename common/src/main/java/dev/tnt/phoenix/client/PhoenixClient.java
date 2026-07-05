package dev.tnt.phoenix.client;

import dev.tnt.phoenix.client.platform.ClientPlatform;
import dev.tnt.phoenix.platform.Services;

public final class PhoenixClient {

    public static final ClientPlatform PLATFORM = Services.load(ClientPlatform.class);

    public static void init() {

    }
}
