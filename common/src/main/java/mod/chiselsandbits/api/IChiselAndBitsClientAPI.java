package mod.chiselsandbits.api;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.platform.ClientApiProvider;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** Client-only Chisels & Bits API surface. */
public interface IChiselAndBitsClientAPI {

    static IChiselAndBitsClientAPI getInstance() {
        return ClientApiProvider.get();
    }

    @NotNull
    KeyMapping getKeyBinding(@NotNull ModKeyBinding modKeyBinding);

    void renderModel(
            PoseStack stack,
            LegacyBakedModel model,
            Level world,
            BlockPos pos,
            int alpha,
            int combinedLight,
            int combinedOverlay);

    void renderGhostModel(
            PoseStack stack,
            LegacyBakedModel model,
            Level world,
            BlockPos pos,
            boolean isUnplaceable,
            int combinedLight,
            int combinedOverlay);
}
