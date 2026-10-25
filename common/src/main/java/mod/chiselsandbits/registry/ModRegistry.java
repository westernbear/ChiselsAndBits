package mod.chiselsandbits.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Loader-neutral registry queue.
 *
 * <p>Entries are collected while the mod constructs. Fabric flushes them immediately
 * ({@link mod.chiselsandbits.platform.RegistryFlush}); NeoForge replays them from its
 * {@code RegisterEvent} because built-in registries are frozen by then.
 *
 * <p>Registries are referenced through keys plus a lazy lookup so that merely queueing an entry
 * never touches {@link BuiltInRegistries} on a loader where it is not bootstrapped yet.
 */
public final class ModRegistry {

    public static final Registrar<Block> BLOCKS = new Registrar<>(Registries.BLOCK, () -> BuiltInRegistries.BLOCK);
    public static final Registrar<Item> ITEMS = new Registrar<>(Registries.ITEM, () -> BuiltInRegistries.ITEM);
    public static final Registrar<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            new Registrar<>(Registries.BLOCK_ENTITY_TYPE, () -> BuiltInRegistries.BLOCK_ENTITY_TYPE);
    public static final Registrar<MenuType<?>> MENU_TYPES =
            new Registrar<>(Registries.MENU, () -> BuiltInRegistries.MENU);
    public static final Registrar<net.minecraft.core.component.DataComponentType<?>> DATA_COMPONENT_TYPES =
            new Registrar<>(Registries.DATA_COMPONENT_TYPE, () -> BuiltInRegistries.DATA_COMPONENT_TYPE);
    public static final Registrar<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            new Registrar<>(Registries.RECIPE_SERIALIZER, () -> BuiltInRegistries.RECIPE_SERIALIZER);
    public static final Registrar<CreativeModeTab> CREATIVE_MODE_TABS =
            new Registrar<>(Registries.CREATIVE_MODE_TAB, () -> BuiltInRegistries.CREATIVE_MODE_TAB);

    private static final List<Entry<?>> ENTRIES = new ArrayList<>();

    private ModRegistry() {
        throw new IllegalStateException("Tried to initialize: ModRegistry but this is a Utility class.");
    }

    public static <T, V extends T> void add(final Registrar<T> registrar, final String name, final Supplier<V> value) {
        ENTRIES.add(new Entry<>(registrar, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), value::get));
    }

    public static List<Entry<?>> entries() {
        return List.copyOf(ENTRIES);
    }

    public record Entry<T>(Registrar<T> registrar, Identifier id, Supplier<T> value) {}

    public static final class Registrar<T> {

        private final ResourceKey<? extends Registry<T>> key;
        private final Supplier<Registry<T>> registry;

        private Registrar(final ResourceKey<? extends Registry<T>> key, final Supplier<Registry<T>> registry) {
            this.key = key;
            this.registry = registry;
        }

        public ResourceKey<? extends Registry<T>> key() {
            return key;
        }

        public Registry<T> registry() {
            return registry.get();
        }
    }
}
