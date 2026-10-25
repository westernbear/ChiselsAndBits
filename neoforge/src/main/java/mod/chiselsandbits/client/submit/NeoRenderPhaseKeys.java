package mod.chiselsandbits.client.submit;

import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

/** Narrow accessors so the bridge does not depend on the concrete (generic) key constants. */
public final class NeoRenderPhaseKeys {

    public static final RenderPhaseKey<SubmitNode> ALWAYS_ON_TOP = RenderPhaseKeys.ALWAYS_ON_TOP;
    public static final RenderPhaseKey<SubmitNode> SHAPE_OUTLINES = RenderPhaseKeys.SHAPE_OUTLINES;
    public static final RenderPhaseKey<SubmitNode> TRANSLUCENT_CUSTOM_GEOMETRY =
            RenderPhaseKeys.TRANSLUCENT_CUSTOM_GEOMETRY;

    private NeoRenderPhaseKeys() {}
}
