package mod.chiselsandbits.client;

import net.fabricmc.fabric.api.client.rendering.v1.FabricOrderedSubmitNodeCollector;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhase;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.submit.SubmitNode;

/** Maps the shared render phases onto Fabric's submit phases. */
public final class FabricRenderPhaseBridge implements RenderPhaseBridge {

    @Override
    public void submit(final SubmitNodeCollector collector, final RenderPhase phase, final SubmitNode node) {
        final SubmitRenderPhase target =
                switch (phase) {
                    case ALWAYS_ON_TOP -> SubmitRenderPhases.ALWAYS_ON_TOP;
                    case SHAPE_OUTLINES -> SubmitRenderPhases.SHAPE_OUTLINES;
                    case TRANSLUCENT_CUSTOM_GEOMETRY -> SubmitRenderPhases.TRANSLUCENT_CUSTOM_GEOMETRY;
                };
        ((FabricOrderedSubmitNodeCollector) collector.order(0)).submitCustom(target, node);
    }
}
