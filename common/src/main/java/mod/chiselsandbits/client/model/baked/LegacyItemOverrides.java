package mod.chiselsandbits.client.model.baked;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Internal replacement for the removed vanilla ItemOverrides class. */
public class LegacyItemOverrides {
    public static final LegacyItemOverrides EMPTY = new LegacyItemOverrides();

    public LegacyBakedModel resolve(
            final LegacyBakedModel originalModel,
            final ItemStack stack,
            @Nullable final ClientLevel level,
            @Nullable final LivingEntity entity,
            final int seed) {
        return originalModel;
    }
}
