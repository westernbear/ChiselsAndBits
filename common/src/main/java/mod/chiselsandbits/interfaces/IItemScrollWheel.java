package mod.chiselsandbits.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IItemScrollWheel {

    void scroll(Player player, ItemStack stack, int dwheel);
}
