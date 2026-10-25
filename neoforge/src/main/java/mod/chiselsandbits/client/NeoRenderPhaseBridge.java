package mod.chiselsandbits.client;

import mod.chiselsandbits.client.submit.NeoRenderPhaseKeys;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;

/** Maps the shared render phases onto NeoForge's render phase keys. */
public final class NeoRenderPhaseBridge implements RenderPhaseBridge {

    @Override
    public void submit(final SubmitNodeCollector collector, final RenderPhase phase, final SubmitNode node) {
        final RenderPhaseKey<SubmitNode> key =
                switch (phase) {
                    case ALWAYS_ON_TOP -> NeoRenderPhaseKeys.ALWAYS_ON_TOP;
                    case SHAPE_OUTLINES -> NeoRenderPhaseKeys.SHAPE_OUTLINES;
                    case TRANSLUCENT_CUSTOM_GEOMETRY -> NeoRenderPhaseKeys.TRANSLUCENT_CUSTOM_GEOMETRY;
                };

        ((OrderedSubmitNodeCollectorExtension) collector.order(0)).submitSpecial(key, node);
    }
}
