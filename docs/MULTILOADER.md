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
- **Architectury Plugin 3.5** (pinned timestamped snapshot) + **Architectury Loom no-remap 1.17.487** for `common` and `fabric` (MC 26.2 is unobfuscated)
- **ModDevGradle 2.0.148** for `neoforge` (Architectury Loom’s `neoForge()` dependency DSL is not available in no-remap mode yet)
- **Shadow** bundles `common` into the Fabric jar (`transformProductionFabric`). NeoForge extracts the transformed common output into its own module instead (see [Registration](#registration)). Architectury and Fzzy Config stay external and are declared as loader dependencies (`fzzy_config` uses the `+neoforge` artifact on NeoForge).

## Building

```bash
./gradlew build
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

Gametests:

```bash
./gradlew :fabric:runGametest
./gradlew :fabric:runClientGametest
./gradlew :neoforge:runGametest
```

Test bodies live in `common/src/gametest/java`; each loader supplies its own entry points (Fabric `@GameTest` annotations, NeoForge `RegisterGameTestsEvent` + `RegisterEvent(TEST_FUNCTION)` with a shared `empty_5` test structure). The one Fabric-only test covers the Fabric transfer API; NeoForge has a capability-based equivalent.

Client render verification:

```bash
CHISELSANDBITS_CLIENT_SMOKE=1 xvfb-run -a ./gradlew :neoforge:runClient
```

The smoke asserts the chiseled block model is the chunk-baked `ChiseledBlockStateModel` (contributing no quads without level context), the bit item model is the dynamic delegate, and the captured frame is non-blank, then exits with a status code. The Fabric client gametest additionally compares rendered pixels in-world.

Both loaders bake placed chiseled blocks into the chunk mesh from one shared `ShapeMesh` per distinct shape (`ShapeMeshes`, stored on the shared `VoxelBlobStateInstance`); nothing is redrawn per frame. `ChiseledEmitter` turns a mesh into quads and skips edge cells hidden by an opaque neighbouring chiseled block.

## Platform abstractions

Loader-specific code lives behind `@ExpectPlatform` types in `mod.chiselsandbits.platform`:

- `RegistryFlush` — replays `ModRegistry` entries (Fabric registers immediately; NeoForge registers from `RegisterEvent`)
- `PlatformFluidUtil`, `PlatformPlayerUtil`
- `ClientApiProvider` — client API surface, implemented by both loaders

Config-directory and mod-loaded lookups call Architectury `dev.architectury.platform.Platform` directly; no platform seam is needed for them.

## Registration

`ModRegistry` queues `(registry key, id, supplier)` entries while the mod constructs and never touches `BuiltInRegistries` eagerly, so it is safe before bootstrap. Fabric flushes the queue during `ChiselsAndBits.init()`; NeoForge replays it in `NeoForgeRegistryFlush` (`RegisterEvent`) because built-in registries are frozen by then.

The NeoForge module compiles against `common` but takes its runtime classes from the Architectury-transformed jar, extracted into the module output (`build/transformed-common`). The extraction must stay inside the module: NeoForge's module class loader rejects calls into classes loaded by a separate class loader (`LinkageError`).

## Mixins

`common/src/main/resources/chiselsandbits.mixins.json` holds the loader-neutral mixins (legacy world repair, harvest rules, entity/block behaviour) plus the shared `KeyMappingAccessor` in its `client` section, and is declared by both loaders — `fabric.mod.json` and the `[[mixins]]` entry in `neoforge.mods.toml`. `chiselsandbits.client.mixins.json` holds the client-only HUD/input/atlas mixins and is declared by Fabric only; NeoForge wires the same hooks from its own events.

The shared config uses `defaultRequire: 0` because NeoForge patches some targets (`Entity`, `LivingEntity`). Fabric applies all 19 entries; NeoForge applies the ones whose targets match (verified with `-Dmixin.debug.verbose=true`: legacy fixer, harvest rules, entity behaviour).

## Capabilities

Fabric registers its transfer hooks from `FabricBitStorageRegistration` / `FabricChiselPrinterRegistration`. NeoForge registers `Capabilities.Fluid.BLOCK`, `Capabilities.Item.BLOCK` and `Capabilities.Fluid.ITEM` for the bit storage in `NeoForgeBitStorageCapabilities`.

`getColor` is a client-render concern; the NeoForge implementation delegates to `ClientFluidColors` on the client and returns `-1` on the server.

## Pick block

Hit-aware pick block is loader-neutral: the client reports the precise hit (`PacketPickBlockHit` → `PickBlockHandler`), and `BlockChiseled#getCloneItemStack` consumes it when vanilla's pick packet arrives. No loader event is involved.

## Client

Client logic is shared: `core/ClientSideImpl`, `client/RenderHelper`, `core/ChiselsAndBitsClient` and the `compat/client` event facades live in `common` and talk to a loader-neutral hub (`client/ClientEvents`, `client/RenderPhaseBridge`, `client/KeyBindings`). Each loader only wires its own events:

| Concern | Fabric | NeoForge |
|---|---|---|
| Level extraction / submits | `LevelExtractionEvents`, `LevelRenderEvents` | `ExtractLevelRenderStateEvent`, `SubmitCustomGeometryEvent` |
| Submit ordering | `SubmitRenderPhases` | `RenderPhaseKeys` via `submitSpecial` |
| Ticks / input | `ClientTickEvents`, `UseBlockCallback` | `ClientTickEvent`, `PlayerInteractEvent.RightClickBlock`, `InputEvent.MouseScrollingEvent` |
| Key mappings | `KeyMappingHelper` | `RegisterKeyMappingsEvent` (queued by `KeyBindings`) |
| Models | `ModelLoadingPlugin` after-bake hooks | `ModelEvent.RegisterLoaders` / `ModifyBakingResult` |
| Block rendering | `FabricBlockStateModel.emitQuads` | `ChiseledBlockStateModel` (`DynamicBlockStateModel.collectParts`) |
| Menus / atlases | `MenuScreens` (fabric access widener), `TextureStitchCallback` | `RegisterMenuScreensEvent`, `TextureAtlasStitchedEvent` |

Fabric loads `chiselsandbits.client.mixins.json` (HUD, mouse, multiplayer game mode, atlas); NeoForge reaches the same hooks through `RenderGuiEvent.Post`, `InputEvent.MouseScrollingEvent` and `TextureAtlasStitchedEvent`, so the two loaders agree on behaviour without sharing the mixins.

NeoForge fires `RegisterKeyMappingsEvent` before `FMLClientSetupEvent`, so `NeoForgeClientBootstrap` runs the shared client initialization from the mod constructor (client dist only); otherwise the Controls menu would stay empty.
