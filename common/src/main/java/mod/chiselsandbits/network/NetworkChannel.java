package mod.chiselsandbits.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import mod.chiselsandbits.network.packets.PacketAccurateSneakPlace;
import mod.chiselsandbits.network.packets.PacketBagGui;
import mod.chiselsandbits.network.packets.PacketBagGuiStack;
import mod.chiselsandbits.network.packets.PacketChisel;
import mod.chiselsandbits.network.packets.PacketClearBagGui;
import mod.chiselsandbits.network.packets.PacketOpenBagGui;
import mod.chiselsandbits.network.packets.PacketPickBlockHit;
import mod.chiselsandbits.network.packets.PacketRotateVoxelBlob;
import mod.chiselsandbits.network.packets.PacketSetChiselMode;
import mod.chiselsandbits.network.packets.PacketSetColor;
import mod.chiselsandbits.network.packets.PacketSortBagGui;
import mod.chiselsandbits.network.packets.PacketSuppressInteraction;
import mod.chiselsandbits.network.packets.PacketUndo;
import mod.chiselsandbits.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class NetworkChannel {

    public void registerCommonMessages() {
        registerServerMessage(PacketChisel.PACKET_TYPE, PacketChisel.STREAM_CODEC);
        registerServerMessage(PacketOpenBagGui.PACKET_TYPE, PacketOpenBagGui.STREAM_CODEC);
        registerServerMessage(PacketSetChiselMode.PACKET_TYPE, PacketSetChiselMode.STREAM_CODEC);
        registerServerMessage(PacketRotateVoxelBlob.PACKET_TYPE, PacketRotateVoxelBlob.STREAM_CODEC);
        registerServerMessage(PacketBagGui.PACKET_TYPE, PacketBagGui.STREAM_CODEC);
        registerServerMessage(PacketUndo.PACKET_TYPE, PacketUndo.STREAM_CODEC);
        registerServerMessage(PacketClearBagGui.PACKET_TYPE, PacketClearBagGui.STREAM_CODEC);
        registerServerMessage(PacketSuppressInteraction.PACKET_TYPE, PacketSuppressInteraction.STREAM_CODEC);
        registerServerMessage(PacketSetColor.PACKET_TYPE, PacketSetColor.STREAM_CODEC);
        registerServerMessage(PacketAccurateSneakPlace.PACKET_TYPE, PacketAccurateSneakPlace.STREAM_CODEC);
        registerServerMessage(PacketPickBlockHit.PACKET_TYPE, PacketPickBlockHit.STREAM_CODEC);
        registerServerMessage(PacketSortBagGui.PACKET_TYPE, PacketSortBagGui.STREAM_CODEC);

        registerClientMessage(PacketBagGuiStack.PACKET_TYPE, PacketBagGuiStack.STREAM_CODEC);
    }

    private <MSG extends ModPacket> void registerServerMessage(
            final CustomPacketPayload.Type<MSG> packetType, final StreamCodec<RegistryFriendlyByteBuf, MSG> codec) {
        NetworkManager.registerC2S(
                packetType, codec, (packet, context) -> packet.server((ServerPlayer) context.getPlayer()));
    }

    private <MSG extends ModPacket> void registerClientMessage(
            final CustomPacketPayload.Type<MSG> packetType, final StreamCodec<RegistryFriendlyByteBuf, MSG> codec) {
        EnvExecutor.runWhenOn(
                Env.CLIENT,
                () -> () -> NetworkManager.registerS2C(packetType, codec, (packet, context) -> packet.client()));
    }

    public void sendToServer(final ModPacket msg) {
        NetworkManager.sendToServer(msg);
    }

    public void sendToPlayer(final ModPacket msg, final ServerPlayer player) {
        NetworkManager.sendToPlayer(player, msg);
    }
}
