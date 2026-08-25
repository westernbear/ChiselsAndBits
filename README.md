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

## Issue #1 — empty Block bit GUI icons

Minecraft 26.2 draws inventory icons through `GuiItemAtlas`, which applies
`ItemTransform` (including a built-in `-0.5` model center) before the bake
`localTransform`. C&B was folding the GUI perspective matrix into
`localTransform` only, so bits rotated around the block corner and were clipped
out of the 16×16 atlas slot — tooltips still worked, but icons looked empty.

The fix routes display transforms through `ItemTransform` again and keeps only
the bake matrix in `localTransform`. Client GameTests assert bit GUI models stay
centered and capture a hotbar screenshot.
