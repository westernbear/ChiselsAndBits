package mod.chiselsandbits.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EventFullBlockRestoration {

    private final Level w;
    private final BlockPos pos;
    private final BlockState restoredState;
    private boolean cancelled;

    public EventFullBlockRestoration(final Level w, final BlockPos pos, final BlockState restoredState) {

        this.w = w;
        this.pos = pos;
        this.restoredState = restoredState;
    }

    public Level getWorld() {
        return w;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return restoredState;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
