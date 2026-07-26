package mod.chiselsandbits.client.model.baked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class BaseBakedItemModel extends BaseBakedPerspectiveModel {
    protected ArrayList<BakedQuad> list = new ArrayList<BakedQuad>();

    @Override
    public final boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public final boolean isGui3d() {
        return true;
    }

    @Override
    public final boolean isCustomRenderer() {
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable final BlockState state, @Nullable final Direction side, final RandomSource rand) {
        if (side != null) {
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public LegacyItemOverrides getOverrides() {
        return LegacyItemOverrides.EMPTY;
    }
}
