package mod.chiselsandbits.modes;

import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public enum TapeMeasureModes implements IToolMode {
    BIT(LocalStrings.TapeMeasureBit),
    BLOCK(LocalStrings.TapeMeasureBlock),
    DISTANCE(LocalStrings.TapeMeasureDistance);

    public final LocalStrings string;
    public boolean isDisabled = false;

    public Object binding;

    TapeMeasureModes(final LocalStrings str) {
        string = str;
    }

    public static TapeMeasureModes getMode(final ItemStack stack) {
        if (stack != null) {
            try {
                final String component = stack.get(ModDataComponents.TOOL_MODE);
                if (component != null) {
                    return valueOf(component);
                }
                final CompoundTag nbt = ModUtil.getTagCompound(stack);
                if (nbt.contains("mode")) {
                    final TapeMeasureModes mode = valueOf(nbt.getStringOr("mode", BIT.name()));
                    stack.set(ModDataComponents.TOOL_MODE, mode.name());
                    return mode;
                }
            } catch (final IllegalArgumentException iae) {
                // nope!
            } catch (final Exception e) {
                Log.logError("Unable to determine mode.", e);
            }
        }

        return TapeMeasureModes.BIT;
    }

    public static TapeMeasureModes castMode(final IToolMode chiselMode) {
        if (chiselMode instanceof TapeMeasureModes) {
            return (TapeMeasureModes) chiselMode;
        }

        return TapeMeasureModes.BIT;
    }

    @Override
    public void setMode(final ItemStack stack) {
        if (stack != null) {
            stack.set(ModDataComponents.TOOL_MODE, name());
        }
    }

    @Override
    public LocalStrings getName() {
        return string;
    }

    @Override
    public boolean isDisabled() {
        return isDisabled;
    }
}
