package mod.chiselsandbits.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the currently bound key; 26.2 has no public accessor. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key")
    InputConstants.Key chiselsandbits$getKey();
}
