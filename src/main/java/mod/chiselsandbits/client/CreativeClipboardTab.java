package mod.chiselsandbits.client;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.registry.ModItemGroups;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreativeClipboardTab implements ICacheClearable {
    private static final List<ItemStack> myWorldItems = new ArrayList<ItemStack>();
    private static final Logger log = LoggerFactory.getLogger(CreativeClipboardTab.class);
    static boolean renewMappings = true;
    private static List<ChiseledData> myCrossItems = new ArrayList<>();
    private static ClipboardStorage clipStorage = null;

    private static CreativeClipboardTab instance;

    private CreativeClipboardTab() {
        ChiselsAndBits.getInstance().addClearable(this);
    }

    public static CreativeClipboardTab getInstance() {
        if (instance == null) {
            return (instance = new CreativeClipboardTab());
        }
        return instance;
    }

    public void load(final File file) {
        clipStorage = new ClipboardStorage(file);
        try {
            myCrossItems = clipStorage.read();
        } catch (IOException e) {
            myCrossItems = Collections.emptyList();
            log.info("Error occurred while reading clipboards", e);
        }
    }

    public void addItem(final ItemStack iss) {
        // this is a client side things.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            final IBitAccess bitData = ChiselsAndBits.getApi().createBitItem(iss);

            if (bitData == null) {
                return;
            }

            final ItemStack is = bitData.getBitsAsItem(null, ItemType.CHISELED_BLOCK, true);

            if (is == null) {
                return;
            }

            // remove duplicates if they exist...
            final ChiseledData itemData = NBTBlobConverter.getComponent(is);
            if (itemData == null) {
                return;
            }

            myCrossItems.remove(itemData);

            // add item to front...
            myCrossItems.add(0, itemData);

            // remove extra items from back..
            while (myCrossItems.size()
                            > ChiselsAndBits.getConfig()
                                    .getServer()
                                    .creativeClipboardSize
                                    .get()
                    && !myCrossItems.isEmpty()) {
                myCrossItems.remove(myCrossItems.size() - 1);
            }

            try {
                clipStorage.write(myCrossItems);
            } catch (IOException e) {
                log.info("Error occurred while saving clipboard", e);
            }
            myWorldItems.clear();
            renewMappings = true;
        }

        ModItemGroups.CLIPBOARD.get().getDisplayItems().clear();
        ModItemGroups.CLIPBOARD.get().getDisplayItems().addAll(getClipboard());
    }

    public List<ItemStack> getClipboard() {
        if (renewMappings) {
            myWorldItems.clear();
            renewMappings = false;

            for (final ChiseledData data : myCrossItems) {
                final NBTBlobConverter c = new NBTBlobConverter();
                c.readChisleData(data, VoxelBlob.VERSION_ANY);

                // recalculate.
                c.updateFromBlob();

                final ItemStack worldItem = c.getItemStack(false);

                if (worldItem != null) {
                    myWorldItems.add(worldItem);
                }
            }
        }

        return ImmutableList.copyOf(myWorldItems);
    }

    @Override
    public void clearCache() {
        renewMappings = true;
    }
}
