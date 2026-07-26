package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.events.EventPlayerInteract;
import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class PacketSuppressInteraction extends ModPacket {

    public static final CustomPacketPayload.Type<PacketSuppressInteraction> PACKET_TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_suppress_interaction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSuppressInteraction> STREAM_CODEC =
            CustomPacketPayload.codec(PacketSuppressInteraction::getPayload, PacketSuppressInteraction::new);

    private boolean newSetting = false;

    public PacketSuppressInteraction(final FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketSuppressInteraction(final boolean newSetting) {
        this.newSetting = newSetting;
    }

    @Override
    public void server(final ServerPlayer player) {
        EventPlayerInteract.setPlayerSuppressionState(player, newSetting);
    }

    @Override
    public void getPayload(final FriendlyByteBuf buffer) {
        buffer.writeBoolean(newSetting);
    }

    @Override
    public void readPayload(final FriendlyByteBuf buffer) {
        newSetting = buffer.readBoolean();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
