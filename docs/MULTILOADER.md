# Multiloader layout

Chisels & Bits targets **Fabric** and **NeoForge** on Minecraft **26.2** using a shared `common` module and loader-specific platform projects.

## Modules

| Module | Role |
|--------|------|
| `common/` | Shared game logic, registry, networking, and public API |
| `fabric/` | Fabric entrypoints, mixins, client rendering, Fabric transfer hooks |
| `neoforge/` | NeoForge entrypoint and platform `@ExpectPlatform` implementations |

## Gradle toolchain

- **Java 25**, **Gradle 9.5**
- **Architectury Plugin 3.5** + **Architectury Loom no-remap** for `common` and `fabric` (MC 26.2 is unobfuscated)
- **ModDevGradle 2.0** for `neoforge` (Architectury Loom’s `neoForge()` dependency DSL is not available in no-remap mode yet)
- **Shadow** bundles `common` into loader jars (`transformProductionFabric` / project dependency on NeoForge)

## Building

```bash
./gradlew build
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

Fabric gametests (when enabled):

```bash
./gradlew :fabric:runGametest
./gradlew :fabric:runClientGametest
```

## Platform abstractions

Loader-specific code lives behind `@ExpectPlatform` types in `mod.chiselsandbits.platform`:

- `PlatformHelper` — config directory, mod-loaded checks (uses Architectury `Platform` on both loaders)
- `PlatformPickBlock`, `PlatformFluidUtil`, `PlatformPlayerUtil`
- `ClientApiProvider` — Fabric-only client API surface today

Fabric-only pieces (mixins, model loading, transfer API registration) stay in `fabric/`. NeoForge still needs client bootstrap, fluid/item capabilities, and model hooks.

## Access widener

Fabric uses `chiselsandbits.accesswidener`. NeoForge ships a converted access transformer when built through ModDevGradle.
