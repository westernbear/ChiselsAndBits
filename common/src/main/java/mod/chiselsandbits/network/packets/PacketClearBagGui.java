package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.bitbag.BagContainer;
import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class PacketClearBagGui extends ModPacket {

    public static final CustomPacketPayload.Type<PacketClearBagGui> PACKET_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_clear_bag_gui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketClearBagGui> STREAM_CODEC =
            CustomPacketPayload.codec(PacketClearBagGui::getPayload, PacketClearBagGui::new);

    private ItemStack stack = null;

    public PacketClearBagGui(final FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketClearBagGui(final ItemStack inHandItem) {
        stack = inHandItem;
    }

    @Override
    public void server(final ServerPlayer player) {
        if (player.containerMenu instanceof BagContainer) {
            ((BagContainer) player.containerMenu).clear(stack);
        }
    }

    @Override
    public void getPayload(final FriendlyByteBuf buffer) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack);
        // no data...
    }

    @Override
    public void readPayload(final FriendlyByteBuf buffer) {
        stack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
