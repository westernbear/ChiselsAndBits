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

public class PacketSortBagGui extends ModPacket {

    public static final CustomPacketPayload.Type<PacketSortBagGui> PACKET_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_sort_bag_gui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSortBagGui> STREAM_CODEC =
            CustomPacketPayload.codec(PacketSortBagGui::getPayload, PacketSortBagGui::new);

    public PacketSortBagGui(FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketSortBagGui() {}

    @Override
    public void server(final ServerPlayer player) {
        if (player.containerMenu instanceof BagContainer) {
            ((BagContainer) player.containerMenu).sort();
        }
    }

    @Override
    public void getPayload(FriendlyByteBuf buffer) {}

    @Override
    public void readPayload(FriendlyByteBuf buffer) {}

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
