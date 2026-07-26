package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class PacketSetChiselMode extends ModPacket {

    public static final CustomPacketPayload.Type<PacketSetChiselMode> PACKET_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_set_chisel_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetChiselMode> STREAM_CODEC =
            CustomPacketPayload.codec(PacketSetChiselMode::getPayload, PacketSetChiselMode::new);

    private IToolMode mode = ChiselMode.SINGLE;
    private ChiselToolType type = ChiselToolType.CHISEL;
    private boolean chatNotification = false;

    public PacketSetChiselMode(FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketSetChiselMode(final IToolMode mode, final ChiselToolType type, final boolean chatNotification) {
        this.mode = mode;
        this.type = type;
        this.chatNotification = chatNotification;
    }

    @Override
    public void server(final ServerPlayer player) {
        final ItemStack ei = player.getMainHandItem();
        if (ei != null && ei.getItem() instanceof IChiselModeItem) {
            final IToolMode originalMode = type.getMode(ei);
            mode.setMode(ei);

            if (originalMode != mode && chatNotification) {
                player.sendSystemMessage(Component.translatable(mode.getName().toString()));
            }
        }
    }

    @Override
    public void getPayload(final FriendlyByteBuf buffer) {
        buffer.writeBoolean(chatNotification);
        buffer.writeEnum(type);
        buffer.writeEnum((Enum<?>) mode);
    }

    @Override
    public void readPayload(final FriendlyByteBuf buffer) {
        chatNotification = buffer.readBoolean();
        type = buffer.readEnum(ChiselToolType.class);

        if (type == ChiselToolType.BIT || type == ChiselToolType.CHISEL) {
            mode = buffer.readEnum(ChiselMode.class);
        } else if (type == ChiselToolType.POSITIVEPATTERN) {
            mode = buffer.readEnum(PositivePatternMode.class);
        } else if (type == ChiselToolType.TAPEMEASURE) {
            mode = buffer.readEnum(TapeMeasureModes.class);
        }
    }

    public IToolMode getMode() {
        return mode;
    }

    public void setMode(final IToolMode mode) {
        this.mode = mode;
    }

    public ChiselToolType getToolType() {
        return type;
    }

    public boolean isChatNotification() {
        return chatNotification;
    }

    public void setChatNotification(final boolean chatNotification) {
        this.chatNotification = chatNotification;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }

    public void setType(final ChiselToolType type) {
        this.type = type;
    }
}
