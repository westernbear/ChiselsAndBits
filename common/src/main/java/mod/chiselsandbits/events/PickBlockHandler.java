package mod.chiselsandbits.events;

import java.util.Map;
import java.util.WeakHashMap;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.helpers.ChiselToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Hit-aware pick block for 26.2's server-authoritative pick packet.
 *
 * <p>The client reports the precise hit through {@code PacketPickBlockHit}; the following vanilla pick packet only
 * carries the block position, so {@link BlockChiseled#getCloneItemStack} consumes the remembered hit here. This path
 * is loader-neutral.
 */
public final class PickBlockHandler {

    private static final double COORDINATE_EPSILON = 0.01D;
    private static final long MAX_PENDING_AGE = 2L;
    private static final Map<ServerPlayer, PendingHit> PENDING_HITS = new WeakHashMap<>();

    private PickBlockHandler() {}

    /**
     * Stores a validated one-shot client hit. The following vanilla pick packet consumes it
     * ({@link #consumeHit}); this method never grants an item directly.
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

    /**
     * Consumes the most recent hit remembered for {@code pos}. Vanilla's pick packet does not carry the hit location
     * or the player, so the pending entry is matched by position and dimension instead.
     */
    public static @Nullable BlockHitResult consumeHit(final ServerLevel level, final BlockPos pos) {
        for (final Map.Entry<ServerPlayer, PendingHit> entry :
                Map.copyOf(PENDING_HITS).entrySet()) {
            final PendingHit pending = entry.getValue();
            if (!pending.dimension().equals(level.dimension())
                    || !pending.hit().getBlockPos().equals(pos)) {
                continue;
            }

            PENDING_HITS.remove(entry.getKey());
            final long age = level.getGameTime() - pending.gameTime();
            return age >= 0L && age <= MAX_PENDING_AGE ? pending.hit() : null;
        }
        return null;
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
