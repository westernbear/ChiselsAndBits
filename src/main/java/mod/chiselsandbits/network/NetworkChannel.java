package mod.chiselsandbits.network;

import mod.chiselsandbits.network.packets.PacketAccurateSneakPlace;
import mod.chiselsandbits.network.packets.PacketBagGui;
import mod.chiselsandbits.network.packets.PacketBagGuiStack;
import mod.chiselsandbits.network.packets.PacketChisel;
import mod.chiselsandbits.network.packets.PacketClearBagGui;
import mod.chiselsandbits.network.packets.PacketOpenBagGui;
import mod.chiselsandbits.network.packets.PacketRotateVoxelBlob;
import mod.chiselsandbits.network.packets.PacketSetChiselMode;
import mod.chiselsandbits.network.packets.PacketSetColor;
import mod.chiselsandbits.network.packets.PacketSortBagGui;
import mod.chiselsandbits.network.packets.PacketSuppressInteraction;
import mod.chiselsandbits.network.packets.PacketUndo;
import mod.chiselsandbits.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class NetworkChannel {

    public void registerCommonMessages() {
        registerServerMessage(PacketChisel.PACKET_TYPE);
        registerServerMessage(PacketOpenBagGui.PACKET_TYPE);
        registerServerMessage(PacketSetChiselMode.PACKET_TYPE);
        registerServerMessage(PacketRotateVoxelBlob.PACKET_TYPE);
        registerServerMessage(PacketBagGui.PACKET_TYPE);
        registerServerMessage(PacketUndo.PACKET_TYPE);
        registerServerMessage(PacketClearBagGui.PACKET_TYPE);
        registerServerMessage(PacketSuppressInteraction.PACKET_TYPE);
        registerServerMessage(PacketSetColor.PACKET_TYPE);
        registerServerMessage(PacketAccurateSneakPlace.PACKET_TYPE);
        registerServerMessage(PacketSortBagGui.PACKET_TYPE);

        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> registerClientMessage(PacketBagGuiStack.PACKET_TYPE));
    }

    private <MSG extends ModPacket> void registerServerMessage(final PacketType<MSG> packetType) {
        ServerPlayNetworking.registerGlobalReceiver(
                packetType, (packet, player, responseSender) -> packet.server(player));
    }

    private <MSG extends ModPacket> void registerClientMessage(final PacketType<MSG> packetType) {
        ClientPlayNetworking.registerGlobalReceiver(packetType, (packet, player, responseSender) -> packet.client());
    }

    public void sendToServer(final ModPacket msg) {
        ClientPlayNetworking.send(msg);
    }

    public void sendToPlayer(final ModPacket msg, final ServerPlayer player) {
        ServerPlayNetworking.send(player, msg);
    }
}
