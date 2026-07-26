package mod.chiselsandbits.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public abstract class ModPacket implements FabricPacket {

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

    @Override
    public void write(FriendlyByteBuf buf) {
        getPayload(buf);
    }
}
