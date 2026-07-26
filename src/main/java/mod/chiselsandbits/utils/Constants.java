package mod.chiselsandbits.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mod.chiselsandbits.core.ChiselsAndBits;
import net.minecraft.resources.ResourceLocation;

public final class Constants {

    public static final String MOD_ID = "chiselsandbits";
    public static final String MOD_NAME = "Chisels & Bits";
    public static final String MOD_VERSION = "%VERSION%";

    private Constants() {
        throw new IllegalStateException("Tried to initialize: Constants but this is a Utility class.");
    }

    public static class DataGenerator {

        public static final Gson GSON =
                new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        public static final String EN_US_LANG = "assets/" + Constants.MOD_ID + "/lang/en_us.json";
        public static final String ITEM_MODEL_DIR = "assets/" + Constants.MOD_ID + "/models/item/";
        public static final ResourceLocation CHISELED_BLOCK_MODEL =
                new ResourceLocation(ChiselsAndBits.MODID, "block/chiseled_block");
        public static final ResourceLocation CHISEL_PRINTER_MODEL =
                new ResourceLocation(ChiselsAndBits.MODID, "block/chisel_printer");
        private static final String DATAPACK_DIR = "data/" + MOD_ID + "/";
        public static final String RECIPES_DIR = DATAPACK_DIR + "recipes/";
        public static final String TAGS_DIR = DATAPACK_DIR + "tags/";
        public static final String BLOCK_TAGS_DIR = TAGS_DIR + "blocks/";
        public static final String ITEM_TAGS_DIR = TAGS_DIR + "items/";
        public static final String LOOT_TABLES_DIR = DATAPACK_DIR + "loot_tables/blocks";
        private static final String RESOURCEPACK_DIR = "assets/" + MOD_ID + "/";
        public static final String BLOCKSTATE_DIR = RESOURCEPACK_DIR + "blockstates/";
    }
}
