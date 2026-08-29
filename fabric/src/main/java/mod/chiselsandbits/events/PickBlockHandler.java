package mod.chiselsandbits.events;

import java.util.Map;
import java.util.WeakHashMap;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.helpers.ChiselToolType;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Restores C&B's hit-aware pick-block behavior on top of 26.2's server-authoritative pick event. */
public final class PickBlockHandler {

    private static final double COORDINATE_EPSILON = 0.01D;
    private static final long MAX_PENDING_AGE = 2L;
    private static final Map<ServerPlayer, PendingHit> PENDING_HITS = new WeakHashMap<>();
    private static boolean registered;

    private PickBlockHandler() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }

        PlayerPickItemEvents.BLOCK.register(PickBlockHandler::onPickBlock);
        registered = true;
    }

    /**
     * Stores a validated one-shot client hit. The following vanilla pick packet consumes it through
     * {@link PlayerPickItemEvents#BLOCK}; this method never grants an item directly.
     */
    public static void rememberHit(final ServerPlayer player, final BlockHitResult hit) {
        PENDING_HITS.remove(player);

        final Vec3 location = hit.getLocation();
        if (!Double.isFinite(location.x) || !Double.isFinite(location.y) || !Double.isFinite(location.z)) {
            return;
        }

        final ServerLevel level = player.level();
        final BlockPos pos = hit.getBlockPos();
        if (!level.hasChunkAt(pos)
                || !(level.getBlockState(pos).getBlock() instanceof BlockChiseled)
                || ChiselToolType.fromItemStack(player.getMainHandItem()) == null
                || !isInsideBlock(location, pos)
                || !isWithinReach(player, location)) {
            return;
        }

        PENDING_HITS.put(player, new PendingHit(level.dimension(), hit, level.getGameTime()));
    }

    private static @Nullable ItemStack onPickBlock(
            final ServerPlayer player, final BlockPos pos, final BlockState state, final boolean includeData) {
        if (!(state.getBlock() instanceof BlockChiseled)) {
            PENDING_HITS.remove(player);
            return null;
        }

        final ServerLevel level = player.level();
        final BlockState currentState = level.getBlockState(pos);
        if (!(currentState.getBlock() instanceof BlockChiseled block)
                || !(level.getBlockEntity(pos) instanceof TileEntityBlockChiseled blockEntity)
                || blockEntity.getLevel() != level) {
            PENDING_HITS.remove(player);
            return ItemStack.EMPTY;
        }

        if (ChiselToolType.fromItemStack(player.getMainHandItem()) == null) {
            PENDING_HITS.remove(player);
            return blockEntity.getItemStack(player);
        }

        BlockHitResult hit = consumePendingHit(player, level, pos);
        if (hit == null) {
            final HitResult reraycast = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (reraycast instanceof BlockHitResult blockHitResult
                    && blockHitResult.getBlockPos().equals(pos)) {
                hit = blockHitResult;
            }
        }

        return hit == null ? ItemStack.EMPTY : block.getPickedBit(hit, blockEntity);
    }

    private static @Nullable BlockHitResult consumePendingHit(
            final ServerPlayer player, final ServerLevel level, final BlockPos pos) {
        final PendingHit pending = PENDING_HITS.remove(player);
        if (pending == null || !pending.dimension().equals(level.dimension())) {
            return null;
        }

        final long age = level.getGameTime() - pending.gameTime();
        if (age < 0L || age > MAX_PENDING_AGE || !pending.hit().getBlockPos().equals(pos)) {
            return null;
        }

        return pending.hit();
    }

    private static boolean isInsideBlock(final Vec3 location, final BlockPos pos) {
        return location.x >= pos.getX() - COORDINATE_EPSILON
                && location.x <= pos.getX() + 1.0D + COORDINATE_EPSILON
                && location.y >= pos.getY() - COORDINATE_EPSILON
                && location.y <= pos.getY() + 1.0D + COORDINATE_EPSILON
                && location.z >= pos.getZ() - COORDINATE_EPSILON
                && location.z <= pos.getZ() + 1.0D + COORDINATE_EPSILON;
    }

    private static boolean isWithinReach(final ServerPlayer player, final Vec3 location) {
        final double permittedReach = player.blockInteractionRange() + COORDINATE_EPSILON;
        return player.getEyePosition().distanceToSqr(location) <= permittedReach * permittedReach;
    }

    private record PendingHit(ResourceKey<Level> dimension, BlockHitResult hit, long gameTime) {}
}
