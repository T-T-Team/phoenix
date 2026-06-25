package dev.tnt.phoenix.platform;

import java.util.Objects;
import java.util.ServiceLoader;

public final class Services {

    public static final Platform PLATFORM = load(Platform.class);

    public static <T> T load(Class<T> itf) {
        ServiceLoader<T> loader = ServiceLoader.load(itf);
        T service = loader.findFirst().orElse(null);
        return Objects.requireNonNull(service, "Unable to load service " + itf.getName());
    }
}
