package mod.chiselsandbits.client;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public class ClipboardStorage {

    private final File file;

    public ClipboardStorage(final File file) {
        this.file = file;
    }

    public void write(final List<ChiseledData> items) throws IOException {
        if (!ChiselsAndBits.getConfig().getClient().persistCreativeClipboard.get()) {
            return;
        }

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }

        final CompoundTag root = new CompoundTag();
        root.putInt("size", items.size());
        for (int i = 0; i < items.size(); i++) {
            root.put(
                    "clipboard_" + i,
                    ChiseledData.CODEC
                            .encodeStart(NbtOps.INSTANCE, items.get(i))
                            .getOrThrow(IOException::new));
        }
        NbtIo.write(root, file.toPath());
    }

    public List<ChiseledData> read() throws IOException {
        if (!ChiselsAndBits.getConfig().getClient().persistCreativeClipboard.get()) {
            return Lists.newArrayList();
        }

        if (!file.getParentFile().exists()) {
            file.mkdir();
            return Lists.newArrayList();
        }

        if (!file.exists()) {
            return Lists.newArrayList();
        }

        final List<ChiseledData> items = new ArrayList<>();
        final CompoundTag root = NbtIo.read(file.toPath());
        if (root == null) {
            return items;
        }
        final int size = root.getIntOr("size", 0);

        for (int i = 0; i < size; i++) {
            final Tag encoded = root.get("clipboard_" + i);
            if (encoded == null) {
                continue;
            }

            final var decoded =
                    ChiseledData.CODEC.parse(NbtOps.INSTANCE, encoded).result();
            if (decoded.isPresent()) {
                items.add(decoded.get());
                continue;
            }

            if (encoded instanceof CompoundTag legacyRoot) {
                final CompoundTag legacyData = legacyRoot.getCompoundOrEmpty(ModUtil.NBT_BLOCKENTITYTAG);
                final CompoundTag source = legacyData.isEmpty() ? legacyRoot : legacyData;
                final NBTBlobConverter converter = new NBTBlobConverter();
                if (converter.readChisleData(source, VoxelBlob.VERSION_ANY)) {
                    final ChiseledData migrated = converter.toComponent(true);
                    if (migrated != null) {
                        items.add(migrated);
                    }
                }
            }
        }

        return items;
    }
}
