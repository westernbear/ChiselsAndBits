package mod.chiselsandbits.compat.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/** C&amp;B block/entity outline events. */
public interface DrawSelectionEvents {

    ClientEvent<Block> BLOCK = ClientEvent.arrayBacked(Block.class, callbacks -> (levelState, outline) -> {
        for (final Block callback : callbacks) {
            if (callback.onHighlightBlock(levelState, outline)) {
                return true;
            }
        }
        return false;
    });

    ClientEvent<Entity> ENTITY = ClientEvent.arrayBacked(Entity.class, callbacks -> (levelState, state) -> {
        for (final Entity callback : callbacks) {
            if (callback.onHighlightEntity(levelState, state)) {
                return true;
            }
        }
        return false;
    });

    static boolean fireBlock(final LevelRenderState levelState, final BlockOutlineRenderState outline) {
        return BLOCK.invoker().onHighlightBlock(levelState, outline);
    }

    @FunctionalInterface
    interface Block {
        boolean onHighlightBlock(LevelRenderState levelState, BlockOutlineRenderState outline);
    }

    @FunctionalInterface
    interface Entity {
        boolean onHighlightEntity(LevelRenderState levelState, EntityRenderState state);
    }
}
