package mod.chiselsandbits.network;

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
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

        PayloadTypeRegistry.clientboundPlay().register(PacketBagGuiStack.PACKET_TYPE, PacketBagGuiStack.STREAM_CODEC);
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> registerClientMessage(PacketBagGuiStack.PACKET_TYPE));
    }

    private <MSG extends ModPacket> void registerServerMessage(
            final CustomPacketPayload.Type<MSG> packetType, final StreamCodec<RegistryFriendlyByteBuf, MSG> codec) {
        PayloadTypeRegistry.serverboundPlay().register(packetType, codec);
        ServerPlayNetworking.registerGlobalReceiver(packetType, (packet, context) -> packet.server(context.player()));
    }

    private <MSG extends ModPacket> void registerClientMessage(final CustomPacketPayload.Type<MSG> packetType) {
        ClientPlayNetworking.registerGlobalReceiver(packetType, (packet, context) -> packet.client());
    }

    public void sendToServer(final ModPacket msg) {
        ClientPlayNetworking.send(msg);
    }

    public void sendToPlayer(final ModPacket msg, final ServerPlayer player) {
        ServerPlayNetworking.send(player, msg);
    }
}
