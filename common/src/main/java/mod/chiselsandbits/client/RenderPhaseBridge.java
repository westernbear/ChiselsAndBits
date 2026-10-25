package mod.chiselsandbits.client;

import java.util.Objects;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.submit.SubmitNode;

/**
 * Submits vanilla submit nodes into a named render phase.
 *
 * <p>Fabric orders submits through {@code SubmitRenderPhases}; NeoForge through {@code RenderPhaseKeys}. Each loader
 * installs its bridge during client setup, so the shared render helpers stay loader-neutral.
 */
public interface RenderPhaseBridge {

    void submit(SubmitNodeCollector collector, RenderPhase phase, SubmitNode node);

    static void install(final RenderPhaseBridge bridge) {
        Holder.INSTANCE = Objects.requireNonNull(bridge);
    }

    static void dispatch(final SubmitNodeCollector collector, final RenderPhase phase, final SubmitNode node) {
        Holder.INSTANCE.submit(collector, phase, node);
    }

    final class Holder {
        private static volatile RenderPhaseBridge INSTANCE = (collector, phase, node) -> {
            throw new IllegalStateException("RenderPhaseBridge was not installed by the loader");
        };

        private Holder() {}
    }
}
