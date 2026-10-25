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

On both loaders, `IChiselAndBitsClientAPI.getInstance()` returns the client API; it must only be called from client code.

## Events

Fabric-only `ChiselsAndBitsEvents` callbacks were replaced with loader-agnostic Architectury events:

| Old (Fabric) | New |
|--------------|-----|
| `ChiselsAndBitsEvents.BIT_MODIFICATION` | `BitModificationEvents.BLOCK_BIT_MODIFICATION` / `BLOCK_BIT_POST_MODIFICATION` |
| ad-hoc Fabric `EventFactory` types | `ResourceRegistrationEvent`, `EntityItemPickupEvent` (Architectury `Event`) |

`BitModificationEvents.FULL_BLOCK_RESTORATION` replaces the removed full-block-restoration callback.

## Server / common API (unchanged surface)

These remain on `IChiselAndBitsAPI`:

- Bit access, brushes, bags, undo groups
- `getParameter(ParameterType)`
- Block providers and item stack handlers

## Pick block

Server-side pick-block memory is loader-neutral: `PacketPickBlockHit` stores the precise hit in `PickBlockHandler`, and `BlockChiseled#getCloneItemStack` consumes it when vanilla 26.2 sends the position-only pick packet. This works identically on Fabric and NeoForge.

## Migration checklist

1. Replace client rendering/keybinding calls with `IChiselAndBitsClientAPI`.
2. Subscribe to `BitModificationEvents` instead of removed Fabric event buses.
3. Avoid importing `net.fabricmc.*` from addon common code; depend on `mod.chiselsandbits.api` only.
4. Test on your target loader — both loaders implement the server entry, client wiring, and fluid/item capabilities.
