package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.platform.PlatformPickBlock;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Carries the client crosshair's exact hit point immediately before the vanilla pick-block request. */
public final class PacketPickBlockHit extends ModPacket {

    public static final CustomPacketPayload.Type<PacketPickBlockHit> PACKET_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_pick_block_hit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPickBlockHit> STREAM_CODEC =
            CustomPacketPayload.codec(PacketPickBlockHit::getPayload, PacketPickBlockHit::new);

    private BlockPos pos;
    private Direction direction;
    private double hitX;
    private double hitY;
    private double hitZ;
    private boolean inside;

    public PacketPickBlockHit(final FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketPickBlockHit(final BlockHitResult hit) {
        pos = hit.getBlockPos();
        direction = hit.getDirection();
        hitX = hit.getLocation().x;
        hitY = hit.getLocation().y;
        hitZ = hit.getLocation().z;
        inside = hit.isInside();
    }

    @Override
    public void server(final ServerPlayer playerEntity) {
        PlatformPickBlock.rememberHit(playerEntity, toHitResult());
    }

    @Override
    public void getPayload(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(direction);
        buffer.writeDouble(hitX);
        buffer.writeDouble(hitY);
        buffer.writeDouble(hitZ);
        buffer.writeBoolean(inside);
    }

    @Override
    public void readPayload(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        direction = buffer.readEnum(Direction.class);
        hitX = buffer.readDouble();
        hitY = buffer.readDouble();
        hitZ = buffer.readDouble();
        inside = buffer.readBoolean();
    }

    private BlockHitResult toHitResult() {
        return new BlockHitResult(new Vec3(hitX, hitY, hitZ), direction, pos, inside);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
