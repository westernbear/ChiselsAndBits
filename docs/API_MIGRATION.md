# API migration (1.3.x → multiloader)

This release modernizes the public API for Fabric + NeoForge. Breaking changes are intentional; update integrations against the notes below.

## Client API split

**Removed from `IChiselAndBitsAPI` / `ChiselAndBitsAPI`:**

- `getKeyBinding(ModKeyBinding)`
- `renderModel(...)`
- `renderGhostModel(...)`

**Use instead:**

```java
IChiselAndBitsClientAPI client = IChiselAndBitsClientAPI.getInstance();
client.getKeyBinding(ModKeyBinding.UNDO);
client.renderModel(...);
client.renderGhostModel(...);
```

On NeoForge, `IChiselAndBitsClientAPI.getInstance()` is not available until the NeoForge client module is implemented.

## Events

Fabric-only `ChiselsAndBitsEvents` callbacks were replaced with loader-agnostic Architectury events:

| Old (Fabric) | New |
|--------------|-----|
| `ChiselsAndBitsEvents.BIT_MODIFICATION` | `BitModificationEvents.BEFORE` / `AFTER` |
| ad-hoc Fabric `EventFactory` types | `ResourceRegistrationEvent`, `EntityItemPickupEvent` (Architectury `Event`) |

## Server / common API (unchanged surface)

These remain on `IChiselAndBitsAPI`:

- Bit access, brushes, bags, undo groups
- `getParameter(ParameterType)`
- Block providers and item stack handlers

## Pick block

Server-side pick-block memory is routed through `PlatformPickBlock` instead of direct Fabric networking.

## Migration checklist

1. Replace client rendering/keybinding calls with `IChiselAndBitsClientAPI`.
2. Subscribe to `BitModificationEvents` instead of removed Fabric event buses.
3. Avoid importing `net.fabricmc.*` from addon common code; depend on `mod.chiselsandbits.api` only.
4. Test on your target loader — NeoForge server entry works; client and fluid/item capabilities are still incomplete.
