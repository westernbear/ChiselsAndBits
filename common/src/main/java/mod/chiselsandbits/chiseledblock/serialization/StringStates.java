package mod.chiselsandbits.chiseledblock.serialization;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class StringStates {

    public static int getStateIDFromName(final String name) {
        final String[] parts = name.split("[?&]");

        parts[0] = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);

        final Block blk = BuiltInRegistries.BLOCK.getValue(Identifier.parse(parts[0]));

        if (blk == null || blk == Blocks.AIR) {
            return 0;
        }

        BlockState state = blk.defaultBlockState();

        if (state == null) {
            return 0;
        }

        // rebuild state...
        for (int x = 1; x < parts.length; ++x) {
            try {
                if (parts[x].length() > 0) {
                    final String[] nameval = parts[x].split("[=]");
                    if (nameval.length == 2) {
                        nameval[0] = URLDecoder.decode(nameval[0], StandardCharsets.UTF_8);
                        nameval[1] = URLDecoder.decode(nameval[1], StandardCharsets.UTF_8);

                        state = withState(state, blk, nameval);
                    }
                }
            } catch (final Exception err) {
                Log.logError("Failed to reload Property from store data : " + name, err);
            }
        }

        return ModUtil.getStateId(state);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState withState(final BlockState state, final Block blk, final String[] nameval) {
        final Property<?> prop = blk.getStateDefinition().getProperty(nameval[0]);
        if (prop == null) {
            Log.info(nameval[0] + " is not a valid property for " + BuiltInRegistries.BLOCK.getKey(blk));
            return state;
        }

        return setPropValue(state, prop, nameval[1]);
    }

    public static <T extends Comparable<T>> BlockState setPropValue(
            BlockState blockState, Property<T> property, String value) {
        final Optional<T> pv = property.getValue(value);
        if (pv.isPresent()) {
            return blockState.setValue(property, pv.get());
        } else {
            Log.info(value + " is not a valid value of " + property.getName() + " for "
                    + BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
            return blockState;
        }
    }

    public static String getNameFromStateID(final int key) {
        final BlockState state = ModUtil.getStateById(key);
        final Block blk = state.getBlock();

        String sname = "air?";

        final StringBuilder stateName = new StringBuilder(URLEncoder.encode(
                Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(blk)).toString(), StandardCharsets.UTF_8));
        stateName.append('?');

        boolean first = true;
        for (final Property<?> p : state.getBlock().getStateDefinition().getProperties()) {
            if (!first) {
                stateName.append('&');
            }

            first = false;

            final Comparable<?> propVal = state.getValue(p);

            String saveAs;
            if (propVal instanceof StringRepresentable) {
                saveAs = ((StringRepresentable) propVal).getSerializedName();
            } else {
                saveAs = propVal.toString();
            }

            stateName.append(URLEncoder.encode(p.getName(), StandardCharsets.UTF_8));
            stateName.append('=');
            stateName.append(URLEncoder.encode(saveAs, StandardCharsets.UTF_8));
        }

        sname = stateName.toString();

        return sname;
    }
}
