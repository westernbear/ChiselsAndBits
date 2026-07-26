package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.network.ModPacket;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public class PacketSetColor extends ModPacket {

    public static final CustomPacketPayload.Type<PacketSetColor> PACKET_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "packet_set_color"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetColor> STREAM_CODEC =
            CustomPacketPayload.codec(PacketSetColor::getPayload, PacketSetColor::new);

    private DyeColor newColor = DyeColor.WHITE;
    private ChiselToolType type = ChiselToolType.TAPEMEASURE;
    private boolean chatNotification = false;

    public PacketSetColor(final FriendlyByteBuf buffer) {
        readPayload(buffer);
    }

    public PacketSetColor(final DyeColor newColor, final ChiselToolType type, final boolean chatNotification) {
        this.newColor = newColor;
        this.type = type;
        this.chatNotification = chatNotification;
    }

    @Override
    public void server(final ServerPlayer player) {
        final ItemStack ei = player.getMainHandItem();
        if (ei != null && ei.getItem() instanceof IChiselModeItem) {
            final DyeColor originalMode = getColor(ei);
            setColor(ei, newColor);

            if (originalMode != newColor && chatNotification) {
                player.sendSystemMessage(Component.translatable("chiselsandbits.color." + newColor.getName()));
            }
        }
    }

    private void setColor(final ItemStack ei, final DyeColor newColor2) {
        if (ei != null) {
            ei.set(ModDataComponents.COLOR, newColor2);
        }
    }

    private DyeColor getColor(final ItemStack ei) {
        try {
            if (ei != null) {
                final DyeColor component = ei.get(ModDataComponents.COLOR);
                if (component != null) {
                    return component;
                }
                final DyeColor legacy =
                        DyeColor.valueOf(ModUtil.getTagCompound(ei).getStringOr("color", DyeColor.WHITE.name()));
                ei.set(ModDataComponents.COLOR, legacy);
                return legacy;
            }
        } catch (final IllegalArgumentException e) {
            // nope!
        }

        return DyeColor.WHITE;
    }

    @Override
    public void getPayload(final FriendlyByteBuf buffer) {
        buffer.writeBoolean(chatNotification);
        buffer.writeEnum(type);
        buffer.writeEnum(newColor);
    }

    @Override
    public void readPayload(final FriendlyByteBuf buffer) {
        chatNotification = buffer.readBoolean();
        type = buffer.readEnum(ChiselToolType.class);
        newColor = buffer.readEnum(DyeColor.class);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
