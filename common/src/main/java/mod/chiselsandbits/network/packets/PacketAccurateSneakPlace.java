package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class PacketAccurateSneakPlace extends ModPacket {

    public static final CustomPacketPayload.Type<PacketAccurateSneakPlace> PACKET_TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_accurate_sneak_place"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAccurateSneakPlace> STREAM_CODEC =
            CustomPacketPayload.codec(PacketAccurateSneakPlace::getPayload, PacketAccurateSneakPlace::new);
    private ItemStack stack;
    private BlockPos pos;
    private InteractionHand hand;
    private Direction side;
    private double hitX, hitY, hitZ;
    private boolean offgrid;

    public PacketAccurateSneakPlace(FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketAccurateSneakPlace(
            final ItemStack stack,
            final BlockPos pos,
            final InteractionHand hand,
            final Direction side,
            final double hitX,
            final double hitY,
            final double hitZ,
            final boolean offgrid) {
        this.stack = stack;
        this.pos = pos;
        this.hand = hand;
        this.side = side;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
        this.offgrid = offgrid;
    }

    @Override
    public void server(final ServerPlayer playerEntity) {
        if (stack != null && stack.getItem() instanceof IItemBlockAccurate) {
            ItemStack inHand = playerEntity.getItemInHand(hand);
            if (ItemStack.isSameItemSameComponents(stack, inHand)) {
                if (playerEntity.isCreative()) {
                    inHand = stack;
                }

                final IItemBlockAccurate ibc = (IItemBlockAccurate) stack.getItem();
                final UseOnContext context = new UseOnContext(
                        playerEntity, hand, new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, pos, false));
                ibc.tryPlace(new BlockPlaceContext(context), offgrid);

                if (!playerEntity.isCreative() && ModUtil.getStackSize(inHand) <= 0) {
                    playerEntity.setItemInHand(hand, ModUtil.getEmptyStack());
                }
            }
        }
    }

    @Override
    public void getPayload(final FriendlyByteBuf buffer) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack);
        buffer.writeBlockPos(pos);
        buffer.writeEnum(side);
        buffer.writeEnum(hand);
        buffer.writeDouble(hitX);
        buffer.writeDouble(hitY);
        buffer.writeDouble(hitZ);
        buffer.writeBoolean(offgrid);
    }

    @Override
    public void readPayload(final FriendlyByteBuf buffer) {
        stack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
        pos = buffer.readBlockPos();
        side = buffer.readEnum(Direction.class);
        hand = buffer.readEnum(InteractionHand.class);
        hitX = buffer.readDouble();
        hitY = buffer.readDouble();
        hitZ = buffer.readDouble();
        offgrid = buffer.readBoolean();
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(final ItemStack stack) {
        this.stack = stack;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(final BlockPos pos) {
        this.pos = pos;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public void setHand(final InteractionHand hand) {
        this.hand = hand;
    }

    public Direction getSide() {
        return side;
    }

    public void setSide(final Direction side) {
        this.side = side;
    }

    public double getHitX() {
        return hitX;
    }

    public void setHitX(final double hitX) {
        this.hitX = hitX;
    }

    public double getHitY() {
        return hitY;
    }

    public void setHitY(final double hitY) {
        this.hitY = hitY;
    }

    public double getHitZ() {
        return hitZ;
    }

    public void setHitZ(final double hitZ) {
        this.hitZ = hitZ;
    }

    public boolean isOffgrid() {
        return offgrid;
    }

    public void setOffgrid(final boolean offgrid) {
        this.offgrid = offgrid;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }

    public interface IItemBlockAccurate {

        InteractionResult tryPlace(UseOnContext context, boolean offGrid);
    }
}
