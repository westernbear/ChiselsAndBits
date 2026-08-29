package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.bitbag.BagContainer;
import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public class PacketOpenBagGui extends ModPacket {

    public static final CustomPacketPayload.Type<PacketOpenBagGui> PACKET_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_open_bag_gui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketOpenBagGui> STREAM_CODEC =
            CustomPacketPayload.codec(PacketOpenBagGui::getPayload, PacketOpenBagGui::new);

    public PacketOpenBagGui(FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketOpenBagGui() {}

    @Override
    public void server(final ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, playerInventory, playerEntity) -> new BagContainer(id, playerInventory),
                Component.literal("Bitbag")));
    }

    @Override
    public void getPayload(final FriendlyByteBuf buffer) {
        // no data...
    }

    @Override
    public void readPayload(final FriendlyByteBuf buffer) {
        // no data..
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
