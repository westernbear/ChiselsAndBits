package mod.chiselsandbits.registry;

import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static final class Items {
        public static final TagKey<Item> CHISEL = tag("chisel");
        public static final TagKey<Item> BIT_BAG = tag("bit_bag");

        private static TagKey<Item> tag(String name) {
            return ItemTags.bind(Constants.MOD_ID + ":" + name);
        }
    }

    public static final class Blocks {
        public static final TagKey<Block> FORCED_CHISELABLE = tag("chiselable/forced");
        public static final TagKey<Block> BLOCKED_CHISELABLE = tag("chiselable/blocked");
        public static final TagKey<Block> CHISELED_BLOCK = tag("chiseled/block");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(Constants.MOD_ID + ":" + name));
        }
    }
}
