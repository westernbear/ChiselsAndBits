package mod.chiselsandbits.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.Nullable;

/**
 * Key mapping registration and lookup shared by both loaders.
 *
 * <p>Fabric registers immediately; NeoForge queues until {@code RegisterKeyMappingsEvent}. Bindings created by this
 * class are also what the client API resolves through {@code IChiselAndBitsClientAPI#getKeyBinding}.
 */
public final class KeyBindings {

    @FunctionalInterface
    public interface Registrar {
        KeyMapping register(String translationKey, InputConstants.Key defaultKey, KeyMapping.Category category);
    }

    private static final List<KeyMapping> CREATED = new ArrayList<>();

    private static volatile Registrar registrar = (translationKey, defaultKey, category) ->
            new KeyMapping(translationKey, defaultKey.getType(), defaultKey.getValue(), category);

    private KeyBindings() {}

    public static void install(final Registrar registration) {
        registrar = Objects.requireNonNull(registration);
    }

    /** Registers (or queues) a mapping and remembers it for later lookup. */
    public static KeyMapping register(
            final String translationKey, final InputConstants.Key defaultKey, final KeyMapping.Category category) {
        final KeyMapping mapping = registrar.register(translationKey, defaultKey, category);
        synchronized (CREATED) {
            CREATED.add(mapping);
        }
        return mapping;
    }

    /** All mappings created so far; used by loaders that must register them in a later event. */
    public static List<KeyMapping> created() {
        synchronized (CREATED) {
            return List.copyOf(CREATED);
        }
    }

    /** The key currently bound to the mapping, or {@code InputConstants.UNKNOWN}. */
    public static InputConstants.Key boundKeyOf(@Nullable final KeyMapping mapping) {
        if (mapping == null) {
            return InputConstants.UNKNOWN;
        }
        return ((mod.chiselsandbits.mixin.KeyMappingAccessor) mapping).chiselsandbits$getKey();
    }
}
