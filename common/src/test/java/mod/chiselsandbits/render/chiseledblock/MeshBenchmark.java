package mod.chiselsandbits.render.chiseledblock;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.culling.ICullTest;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.legacy.LegacyChiseledBlockFix;
import mod.chiselsandbits.render.chiseledblock.baseline.LegacyFaceRegion;
import mod.chiselsandbits.render.chiseledblock.baseline.LegacyFaceRegionExtractor;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Replays the old and new chiseled meshers on every chiseled block of a real 1.12 world.
 *
 * <p>Run with {@code ./gradlew :common:meshBenchmark -PbenchWorld=<world dir> [-PbenchRadius=8]}; it also reports
 * the heaviest render-distance window of the world. Legacy block ids above 255 (modded) become stone. The old cost is a lower bound: it ran once per render type a block used, this replays one.
 */
public final class MeshBenchmark {
    private static final int OLD_MODEL_CACHE = 1000;
    private static final int ROUNDS = 3;

    private static final Map<Integer, Integer> LEGACY_STATES = new HashMap<>();
    private static final ICullTest OLD_CULL = MeshBenchmark::oldVisible;
    /** The new cull test adds the opaque-neighbour rule of {@code MCCullTest}. */
    private static final ICullTest NEW_CULL = (state, neighbour) -> !opaque(neighbour) && oldVisible(state, neighbour);

    private MeshBenchmark() {}

    public static void main(final String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        final int radius = args.length > 1 && !args[1].isEmpty() ? Integer.parseInt(args[1]) : 8;

        final List<VoxelBlob> shapes = new ArrayList<>();
        final Map<ByteBuffer, Integer> shapeIds = new HashMap<>();
        final Map<Long, Integer> placed = new HashMap<>();
        final List<Long> loadOrder = new ArrayList<>();
        try (Stream<Path> regions = Files.list(Path.of(args[0], "region"))) {
            for (final Path region :
                    regions.filter(p -> p.toString().endsWith(".mca")).sorted().toList()) {
                for (final CompoundTag chunk : chunks(region)) {
                    final ListTag entities = chunk.getCompoundOrEmpty("Level").getListOrEmpty("TileEntities");
                    for (int index = 0; index < entities.size(); index++) {
                        final CompoundTag entity = entities.getCompoundOrEmpty(index);
                        final byte[] bytes = entity.getByteArray("X").orElse(null);
                        if (bytes == null || !entity.getStringOr("id", "").contains("tileentitychiseled")) {
                            continue;
                        }

                        final ByteBuffer key = ByteBuffer.wrap(bytes);
                        Integer shape = shapeIds.get(key);
                        if (shape == null) {
                            shape = shapes.size();
                            shapes.add(LegacyChiseledBlockFix.decodeLegacyCompact(bytes, MeshBenchmark::legacyState));
                            shapeIds.put(key, shape);
                        }
                        final long pos = BlockPos.asLong(
                                entity.getIntOr("x", 0), entity.getIntOr("y", 0), entity.getIntOr("z", 0));
                        if (placed.put(pos, shape) == null) {
                            loadOrder.add(pos);
                        }
                    }
                }
            }
        }

        final int count = shapes.size();
        final int[] oldQuads = new int[count];
        final ShapeMesh[] meshes = new ShapeMesh[count];
        final double oldMs = time(() -> {
            for (int shape = 0; shape < count; shape++) {
                oldQuads[shape] = oldMesh(shapes.get(shape));
            }
        });
        final double newMs = time(() -> {
            for (int shape = 0; shape < count; shape++) {
                meshes[shape] = ShapeMesher.mesh(shapes.get(shape), NEW_CULL, MeshBenchmark::flags, 1);
            }
        });

        final Stats world = stats(loadOrder, placed, oldQuads, meshes);
        System.out.printf("chiseled blocks      %,d (%,d distinct shapes)%n", world.blocks, count);
        System.out.printf(
                "mesh time per shape  old %.1f us, new %.1f us (%.1fx)%n",
                oldMs * 1000 / count, newMs * 1000 / count, oldMs / newMs);
        System.out.printf("mesh all shapes      old %.0f ms, new %.0f ms (one thread)%n", oldMs, newMs);
        world.print("whole world");
        final int misses = fifo(loadOrder, placed, new LinkedHashSet<>(), null);
        System.out.printf(
                "meshes built, one pass in load order: old %,d (%d-entry FIFO cache, %.1f%% misses), new %,d%n",
                misses, OLD_MODEL_CACHE, 100.0 * misses / loadOrder.size(), count);

        // The heaviest view: the (2r+1)^2-chunk window, a render distance of r, holding the most old quads.
        final Map<Long, Long> chunkQuads = new HashMap<>();
        for (final long pos : loadOrder) {
            chunkQuads.merge(chunk(pos), (long) oldQuads[placed.get(pos)], Long::sum);
        }
        final long[] centre = hottest(chunkQuads, radius);
        final List<Long> zone = new ArrayList<>();
        for (final long pos : loadOrder) {
            final long chunk = chunk(pos);
            if (Math.abs(chunkX(chunk) - centre[0]) <= radius && Math.abs(chunkZ(chunk) - centre[1]) <= radius) {
                zone.add(pos);
            }
        }
        final List<Integer> zoneShapes = new ArrayList<>(
                new LinkedHashSet<>(zone.stream().map(placed::get).toList()));
        System.out.printf(
                "%nhot zone: chunks x %d..%d, z %d..%d (render distance %d, centre block %d, %d)%n",
                centre[0] - radius,
                centre[0] + radius,
                centre[1] - radius,
                centre[1] + radius,
                radius,
                centre[0] * 16 + 8,
                centre[1] * 16 + 8);
        final Stats hot = stats(zone, placed, oldQuads, meshes);
        System.out.printf("chiseled blocks      %,d (%,d distinct shapes)%n", hot.blocks, zoneShapes.size());
        hot.print("hot zone");

        // Entering the zone meshes every chunk once: the old path rebuilt a model per cache miss.
        final int[] zoneMisses = new int[1];
        final double oldEnterMs = time(() -> zoneMisses[0] = fifo(zone, placed, new LinkedHashSet<>(), shapes));
        final double newEnterMs = time(() -> {
            for (final int shape : zoneShapes) {
                ShapeMesher.mesh(shapes.get(shape), NEW_CULL, MeshBenchmark::flags, 1);
            }
        });
        System.out.printf(
                "mesh to enter zone   old %,d builds %.0f ms, new %,d builds %.0f ms (%.1fx, one thread)%n",
                zoneMisses[0], oldEnterMs, zoneShapes.size(), newEnterMs, oldEnterMs / newEnterMs);

        // The old NeoForge renderer redrew every block each frame through the same cache.
        final LinkedHashSet<Integer> warm = new LinkedHashSet<>();
        fifo(zone, placed, warm, null);
        final int frameMisses = fifo(zone, placed, new LinkedHashSet<>(warm), null);
        final double frameMs = time(() -> fifo(zone, placed, new LinkedHashSet<>(warm), shapes));
        System.out.printf(
                "old per-frame redraw: %,d model rebuilds, %.0f ms of meshing per frame; new: none%n",
                frameMisses, frameMs);
    }

    /** Old and new quad totals over some placed blocks. */
    private record Stats(int blocks, long oldQuads, long newQuads, long neighbourQuads) {
        void print(final String label) {
            System.out.printf(
                    "quads per block      old %.1f, new %.1f, new with chiseled neighbours %.1f%n",
                    (double) oldQuads / blocks, (double) newQuads / blocks, (double) neighbourQuads / blocks);
            System.out.printf(
                    "quads %-14s old %,d, new %,d (%.1f%% fewer)%n",
                    label, oldQuads, neighbourQuads, 100.0 * (oldQuads - neighbourQuads) / oldQuads);
        }
    }

    private static Stats stats(
            final List<Long> positions,
            final Map<Long, Integer> placed,
            final int[] oldQuads,
            final ShapeMesh[] meshes) {
        long old = 0;
        long plain = 0;
        long withNeighbours = 0;
        for (final long position : positions) {
            final ShapeMesh mesh = meshes[placed.get(position)];
            old += oldQuads[placed.get(position)];
            plain += mesh.rectCount();
            withNeighbours += mesh.rects.length;
            final BlockPos pos = BlockPos.of(position);
            for (final Direction direction : Direction.values()) {
                final Integer beside = placed.get(pos.relative(direction).asLong());
                final short[] covered = beside == null
                        ? null
                        : meshes[beside].opaqueRows[direction.getOpposite().get3DDataValue()];
                final long[] rects = ShapeMesher.coveredEdgeRects(mesh, direction.get3DDataValue(), covered);
                withNeighbours += rects == null ? 0 : rects.length;
            }
        }
        return new Stats(positions.size(), old, plain, withNeighbours);
    }

    /** Best of {@link #ROUNDS} single-threaded runs, after one warm-up run. */
    private static double time(final Runnable pass) {
        double best = Double.MAX_VALUE;
        for (int round = 0; round <= ROUNDS; round++) {
            final long start = System.nanoTime();
            pass.run();
            final double ms = (System.nanoTime() - start) / 1e6;
            if (round > 0) {
                best = Math.min(best, ms);
            }
        }
        return best;
    }

    /** What the old {@code ChiseledBlockBakedModel} did per render type: copy, extract regions, merge. */
    private static int oldMesh(final VoxelBlob blob) {
        int quads = 0;
        for (final ArrayList<LegacyFaceRegion> plane :
                LegacyFaceRegionExtractor.extract(new VoxelBlob(blob), OLD_CULL)) {
            LegacyFaceRegion.merge(plane);
            quads += plane.size();
        }
        return quads;
    }

    /**
     * Replays the old {@link #OLD_MODEL_CACHE}-entry FIFO model cache over {@code order}, meshing each miss when
     * {@code shapes} is given.
     *
     * @return the misses
     */
    private static int fifo(
            final List<Long> order,
            final Map<Long, Integer> placed,
            final LinkedHashSet<Integer> cache,
            final @Nullable List<VoxelBlob> shapes) {
        int misses = 0;
        for (final long pos : order) {
            final int shape = placed.get(pos);
            if (!cache.contains(shape)) {
                misses++;
                if (shapes != null) {
                    oldMesh(shapes.get(shape));
                }
                cache.add(shape);
                if (cache.size() > OLD_MODEL_CACHE) {
                    cache.remove(cache.iterator().next());
                }
            }
        }
        return misses;
    }

    private static long chunk(final long pos) {
        return ChunkPos.pack(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4);
    }

    private static int chunkX(final long chunk) {
        return ChunkPos.getX(chunk);
    }

    private static int chunkZ(final long chunk) {
        return ChunkPos.getZ(chunk);
    }

    /** The chunk whose (2r+1)^2 window holds the most quads, by a 2D prefix sum over the occupied bounds. */
    private static long[] hottest(final Map<Long, Long> chunkQuads, final int radius) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final long chunk : chunkQuads.keySet()) {
            minX = Math.min(minX, chunkX(chunk));
            minZ = Math.min(minZ, chunkZ(chunk));
            maxX = Math.max(maxX, chunkX(chunk));
            maxZ = Math.max(maxZ, chunkZ(chunk));
        }

        final int width = maxX - minX + 1;
        final int depth = maxZ - minZ + 1;
        final long[][] sum = new long[width + 1][depth + 1];
        for (final Map.Entry<Long, Long> entry : chunkQuads.entrySet()) {
            sum[chunkX(entry.getKey()) - minX + 1][chunkZ(entry.getKey()) - minZ + 1] = entry.getValue();
        }
        for (int x = 1; x <= width; x++) {
            for (int z = 1; z <= depth; z++) {
                sum[x][z] += sum[x - 1][z] + sum[x][z - 1] - sum[x - 1][z - 1];
            }
        }

        long best = -1;
        final long[] centre = new long[2];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                final int x0 = Math.max(0, x - radius);
                final int z0 = Math.max(0, z - radius);
                final int x1 = Math.min(width, x + radius + 1);
                final int z1 = Math.min(depth, z + radius + 1);
                final long quads = sum[x1][z1] - sum[x0][z1] - sum[x1][z0] + sum[x0][z0];
                if (quads > best) {
                    best = quads;
                    centre[0] = x + minX;
                    centre[1] = z + minZ;
                }
            }
        }
        return centre;
    }

    /** The old {@code MCCullTest.isVisible}. */
    private static boolean oldVisible(final int state, final int neighbour) {
        if (state == 0 || state == neighbour) {
            return false;
        }

        final BlockState a = ModUtil.getStateById(state);
        final BlockState b = ModUtil.getStateById(neighbour);
        if (a.getBlock().getClass() == StainedGlassBlock.class && a.getBlock() == b.getBlock()) {
            return false;
        }
        return a.getBlock() instanceof LiquidBlock || !a.skipRendering(b, Direction.NORTH);
    }

    private static boolean opaque(final int state) {
        return state != 0 && ModUtil.getStateById(state).isSolidRender();
    }

    private static int flags(final int state) {
        final BlockState blockState = ModUtil.getStateById(state);
        return 1
                | (blockState.getBlock() instanceof LiquidBlock ? ShapeMesher.FLAG_FLUID : 0)
                | (opaque(state) ? ShapeMesher.FLAG_OPAQUE : 0);
    }

    private static int legacyState(final int legacy) {
        return LEGACY_STATES.computeIfAbsent(legacy, ignored -> {
            final int blockId = legacy & 4095;
            if (legacy == 0) {
                return 0;
            }
            if (blockId > 255) {
                return ModUtil.getStateId(Blocks.STONE.defaultBlockState());
            }

            final Tag tag = BlockStateData.getTag(blockId << 4 | legacy >>> 12 & 15)
                    .convert(NbtOps.INSTANCE)
                    .getValue();
            return tag instanceof CompoundTag compound
                    ? ModUtil.getStateId(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, compound))
                    : 0;
        });
    }

    /** Every chunk of an Anvil region file. */
    private static List<CompoundTag> chunks(final Path region) throws IOException {
        final List<CompoundTag> chunks = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(region.toFile(), "r")) {
            if (file.length() < 8192) {
                return chunks;
            }

            final int[] offsets = new int[1024];
            for (int index = 0; index < offsets.length; index++) {
                offsets[index] = file.readInt();
            }
            for (final int location : offsets) {
                if (location == 0) {
                    continue;
                }

                file.seek((long) (location >>> 8) * 4096);
                final int length = file.readInt();
                if (length <= 1) {
                    continue;
                }
                final int compression = file.readByte();
                final byte[] data = new byte[length - 1];
                file.readFully(data);
                final InputStream raw = new ByteArrayInputStream(data);
                final InputStream inflated = compression == 1 ? new GZIPInputStream(raw) : new InflaterInputStream(raw);
                chunks.add(NbtIo.read(new DataInputStream(new BufferedInputStream(inflated))));
            }
        }
        return chunks;
    }
}
