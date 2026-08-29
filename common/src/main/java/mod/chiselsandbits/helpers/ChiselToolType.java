package mod.chiselsandbits.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import mod.chiselsandbits.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public enum ChiselToolType {
    CHISEL(true, true),
    BIT(true, false),

    CHISELED_BLOCK(true, false),

    POSITIVEPATTERN(true, true),
    TAPEMEASURE(true, true),
    NEGATIVEPATTERN(true, false),
    MIRRORPATTERN(false, false);

    private final boolean hasMenu;
    private final boolean hasItemSettings;

    ChiselToolType(final boolean menu, final boolean itemSettings) {
        hasMenu = menu;
        hasItemSettings = itemSettings;
    }

    /**
     * Resolves the C&B tool contract shared by client interactions and server-authoritative pick-block handling.
     */
    public static @Nullable ChiselToolType fromItemStack(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        if (stack.getItem() instanceof ItemChisel) {
            return CHISEL;
        }

        if (stack.getItem() == ModItems.ITEM_BLOCK_BIT.get()) {
            return BIT;
        }

        if (stack.getItem() instanceof ItemBlockChiseled) {
            return CHISELED_BLOCK;
        }

        if (stack.getItem() == ModItems.ITEM_TAPE_MEASURE.get()) {
            return TAPEMEASURE;
        }

        if (stack.getItem() == ModItems.ITEM_POSITIVE_PRINT.get()
                || stack.getItem() == ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get()) {
            return POSITIVEPATTERN;
        }

        if (stack.getItem() == ModItems.ITEM_NEGATIVE_PRINT.get()
                || stack.getItem() == ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get()) {
            return NEGATIVEPATTERN;
        }

        if (stack.getItem() == ModItems.ITEM_MIRROR_PRINT.get()
                || stack.getItem() == ModItems.ITEM_MIRROR_PRINT_WRITTEN.get()) {
            return MIRRORPATTERN;
        }

        return null;
    }

    public IToolMode getMode(final ItemStack ei) {
        if (this == CHISEL) {
            return ChiselMode.getMode(ei);
        }

        if (this == POSITIVEPATTERN) {
            return PositivePatternMode.getMode(ei);
        }

        if (this == ChiselToolType.TAPEMEASURE) {
            return TapeMeasureModes.getMode(ei);
        }

        throw new NullPointerException();
    }

    public boolean hasMenu() {
        return hasMenu;
    }

    public List<IToolMode> getAvailableModes() {
        if (isBitOrChisel()) {
            final List<IToolMode> modes = new ArrayList<IToolMode>();
            final EnumSet<ChiselMode> used = EnumSet.noneOf(ChiselMode.class);
            final ChiselMode[] orderedModes = {
                ChiselMode.SINGLE,
                ChiselMode.LINE,
                ChiselMode.PLANE,
                ChiselMode.CONNECTED_PLANE,
                ChiselMode.CONNECTED_MATERIAL,
                ChiselMode.DRAWN_REGION,
                ChiselMode.SAME_MATERIAL
            };

            for (final ChiselMode mode : orderedModes) {
                if (!mode.isDisabled) {
                    modes.add(mode);
                    used.add(mode);
                }
            }

            for (final ChiselMode mode : ChiselMode.values()) {
                if (!mode.isDisabled && !used.contains(mode)) {
                    modes.add(mode);
                }
            }

            return modes;
        } else if (this == POSITIVEPATTERN) {
            return asArray(PositivePatternMode.values());
        } else if (this == TAPEMEASURE) {
            return asArray(TapeMeasureModes.values());
        } else {
            return Collections.emptyList();
        }
    }

    private List<IToolMode> asArray(final Object[] values) {
        return Arrays.asList((IToolMode[]) values);
    }

    public boolean isBitOrChisel() {
        return this == BIT || this == ChiselToolType.CHISEL;
    }

    public boolean hasPerToolSettings() {
        return hasItemSettings;
    }

    public boolean requiresPerToolSettings() {
        return this == ChiselToolType.POSITIVEPATTERN || this == ChiselToolType.TAPEMEASURE;
    }
}
