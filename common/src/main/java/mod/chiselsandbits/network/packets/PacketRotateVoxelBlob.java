package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.interfaces.IVoxelBlobItem;
import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;

public class PacketRotateVoxelBlob extends ModPacket {

    public static final CustomPacketPayload.Type<PacketRotateVoxelBlob> PACKET_TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_rotate_voxel_blob"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRotateVoxelBlob> STREAM_CODEC =
            CustomPacketPayload.codec(PacketRotateVoxelBlob::getPayload, PacketRotateVoxelBlob::new);

    private Direction.Axis axis;
    private Rotation rotation;

    public PacketRotateVoxelBlob(FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketRotateVoxelBlob(final Direction.Axis axis, final Rotation rotation) {
        this.axis = axis;
        this.rotation = rotation;
    }

    @Override
    public void server(final ServerPlayer player) {
        final ItemStack is = player.getMainHandItem();
        if (is != null && is.getItem() instanceof IVoxelBlobItem) {
            ((IVoxelBlobItem) is.getItem()).rotate(is, axis, rotation);
        }
    }

    @Override
    public void getPayload(final FriendlyByteBuf buffer) {
        buffer.writeEnum(axis);
        buffer.writeEnum(rotation);
    }

    @Override
    public void readPayload(final FriendlyByteBuf buffer) {
        axis = buffer.readEnum(Direction.Axis.class);
        rotation = buffer.readEnum(Rotation.class);
    }

    public Direction.Axis getAxis() {
        return axis;
    }

    public void setAxis(final Direction.Axis axis) {
        this.axis = axis;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public void setRotation(final Rotation rotation) {
        this.rotation = rotation;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
