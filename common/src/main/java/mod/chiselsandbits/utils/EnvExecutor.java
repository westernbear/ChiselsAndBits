package mod.chiselsandbits.utils;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.function.Supplier;

public final class EnvExecutor {

    private EnvExecutor() {}

    public static void runWhenOn(Env type, Supplier<Runnable> task) {
        if (Platform.getEnvironment() == type) {
            task.get().run();
        }
    }

    public static <T> T callWhenOn(Env type, Supplier<Supplier<T>> callable) {
        if (Platform.getEnvironment() == type) {
            return callable.get().get();
        }
        return null;
    }

    public static <T> T unsafeRunForDist(Supplier<Supplier<T>> client, Supplier<Supplier<T>> server) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            return client.get().get();
        }
        return server.get().get();
    }
}
