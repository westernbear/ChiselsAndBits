# Chisels and Bits (Fabric 26.2)

Fabric port of Chisels and Bits for Minecraft 26.2. Carve blocks into 16³ voxel bits, place them, and sculpt detailed builds.

## Requirements

- JDK 25+
- Fabric Loader 0.19.3+
- Fabric API `0.155.2+26.2`
- Fzzy Config `0.7.6+26.2`

## Build

```bash
./gradlew build
```

The mod jar is written to `build/libs/`.

## Run / test

```bash
# Client
./gradlew runClient

# Server
./gradlew runServer

# Unit / mesh checks + Fabric GameTests
./gradlew check

# Client GameTests (headless UI / item-model checks)
./gradlew runClientGametest
```

## Performance

"Before" is the face-region mesher inherited from the original Chisels & Bits, as this port shipped it before the
rendering rewrite; "After" is the current one. Both run on the same decoded shapes of a 1.12 survival world with
**522,404 chiseled blocks** (50,666 distinct shapes), single-threaded on an Intel N100:

```bash
./gradlew :common:meshBenchmark -PbenchWorld=/path/to/world [-PbenchRadius=8]
```

**Heaviest view**: the 17×17-chunk window (render distance 8) holding the most geometry, with 82,872 chiseled
blocks and 7,394 distinct shapes. Times are the median of three runs.

| | Before | After | |
|---|---:|---:|---|
| Quads to draw | 19.3 M | 10.0 M | 48% fewer |
| Quads per chiseled block | 232.6 | 120.9 | |
| Meshing to load the area | 859 ms (11,193 builds) | 258 ms (7,394 builds) | 3.3× faster |

**Whole world**

| | Before | After | |
|---|---:|---:|---|
| Face extraction + merge, per shape | 73–89 µs | 31–34 µs | 2.2–2.8× faster |
| Quads per chiseled block | 178.6 | 90.9 | 49% fewer |
| Quads in total | 93.3 M | 47.5 M | 49% fewer |
| Meshes built loading the world once | 81,705 | 50,666 | one per distinct shape |

**This port's earlier NeoForge build** (not the original mod) redrew every chiseled block each frame through a block
entity renderer and the 1,000-entry FIFO model cache. Replaying that path in the benchmark over the heaviest view
rebuilds 11,122 models, about 844 ms of meshing, per frame. This is a simulation, not an in-game measurement. Both
loaders now bake chiseled blocks into the chunk mesh, which is rebuilt only when a chunk changes, so this cost is gone.

The "Before" column replays one render type per block, so it is a lower bound: the original meshed each render type a
block used. Times vary about ±10% between runs. In-game FPS depends on the GPU and render distance and is not part of
this benchmark.

What changed:

- Binary greedy meshing over 16-bit occupancy rows per palette state, instead of face-region lists merged pairwise.
- Faces between opaque bits inside a block, and block edges covered by an opaque neighbouring chiseled block, are not
  emitted.
- One mesh per distinct shape, kept on the shared shape instance (no size-bounded cache to thrash) and baked into the
  chunk mesh on both Fabric and NeoForge.
