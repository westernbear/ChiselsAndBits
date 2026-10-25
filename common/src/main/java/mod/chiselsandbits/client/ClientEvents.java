package mod.chiselsandbits.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Loader-neutral client event hub.
 *
 * <p>Each loader fires these from its own event API (Fabric client events / NeoForge client events); the shared client
 * logic in {@code ClientSideImpl} only subscribes here.
 */
public final class ClientEvents {

    public record ExtractContext(
            LevelRenderState levelState, ClientLevel level, DeltaTracker deltaTracker, CameraRenderState camera) {}

    public record SubmitContext(
            LevelRenderState levelState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {}

    public record UseBlockContext(Player player, Level level, InteractionHand hand, BlockHitResult hit) {}

    @FunctionalInterface
    public interface UseBlockHandler {
        InteractionResult use(UseBlockContext context);
    }

    public static final List<Consumer<ExtractContext>> EXTRACT = new CopyOnWriteArrayList<>();
    public static final List<Consumer<SubmitContext>> SUBMIT = new CopyOnWriteArrayList<>();
    public static final List<Runnable> START_TICK = new CopyOnWriteArrayList<>();
    public static final List<Runnable> END_TICK = new CopyOnWriteArrayList<>();
    public static final List<UseBlockHandler> USE_BLOCK = new CopyOnWriteArrayList<>();

    private ClientEvents() {}

    public static void fireExtract(final ExtractContext context) {
        for (final Consumer<ExtractContext> listener : EXTRACT) {
            listener.accept(context);
        }
    }

    public static void fireSubmit(final SubmitContext context) {
        for (final Consumer<SubmitContext> listener : SUBMIT) {
            listener.accept(context);
        }
    }

    public static void fireStartTick() {
        for (final Runnable listener : START_TICK) {
            listener.run();
        }
    }

    public static void fireEndTick() {
        for (final Runnable listener : END_TICK) {
            listener.run();
        }
    }

    public static InteractionResult fireUseBlock(final UseBlockContext context) {
        for (final UseBlockHandler listener : USE_BLOCK) {
            final InteractionResult result = listener.use(context);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }
}
