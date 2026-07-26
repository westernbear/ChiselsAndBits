package mod.chiselsandbits.compat.client;

import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;

/**
 * C&B outline compatibility events.
 *
 * <p>The callback contract remains the same as before: return {@code true} to
 * cancel vanilla's outline. Minecraft/Fabric 26.2 moved block outline drawing
 * into {@link LevelRenderEvents#BEFORE_BLOCK_OUTLINE}, so the bridge reverses
 * that value for Fabric's "return true to keep rendering" contract.
 */
public interface DrawSelectionEvents {
    AtomicBoolean FABRIC_BRIDGE_REGISTERED = new AtomicBoolean();

    Event<Block> BLOCK = EventFactory.createArrayBacked(Block.class, callbacks -> (context, outline) -> {
        for (final Block callback : callbacks) {
            if (callback.onHighlightBlock(context, outline)) {
                return true;
            }
        }
        return false;
    });

    Event<Entity> ENTITY = EventFactory.createArrayBacked(Entity.class, callbacks -> (context, state) -> {
        for (final Entity callback : callbacks) {
            if (callback.onHighlightEntity(context, state)) {
                return true;
            }
        }
        return false;
    });

    static void registerFabricBridge() {
        if (FABRIC_BRIDGE_REGISTERED.compareAndSet(false, true)) {
            LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(
                    (context, outline) -> !BLOCK.invoker().onHighlightBlock(context, outline));
        }
    }

    static boolean fireEntity(final LevelRenderContext context, final EntityRenderState state) {
        return ENTITY.invoker().onHighlightEntity(context, state);
    }

    @FunctionalInterface
    interface Block {
        boolean onHighlightBlock(LevelRenderContext context, BlockOutlineRenderState outline);
    }

    @FunctionalInterface
    interface Entity {
        boolean onHighlightEntity(LevelRenderContext context, EntityRenderState state);
    }
}
