package mod.chiselsandbits.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public abstract class ModPacket implements CustomPacketPayload {

    public ModPacket() {}

    public ModPacket(FriendlyByteBuf buf) {
        readPayload(buf);
    }

    public void server(final ServerPlayer playerEntity) {
        throw new RuntimeException(getClass().getName() + " is not a server packet.");
    }

    public void client() {
        throw new RuntimeException(getClass().getName() + " is not a client packet.");
    }

    public abstract void getPayload(FriendlyByteBuf buffer);

    public abstract void readPayload(FriendlyByteBuf buffer);
}
