package mod.chiselsandbits.modes;

import mod.chiselsandbits.helpers.LocalStrings;
import net.minecraft.world.item.ItemStack;

public interface IToolMode {

    void setMode(ItemStack ei);

    LocalStrings getName();

    String name();

    boolean isDisabled();

    int ordinal();

    /** The client key mapping registered for this mode; unset on a dedicated server. */
    Object binding();
}
