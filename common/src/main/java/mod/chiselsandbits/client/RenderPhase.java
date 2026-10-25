package mod.chiselsandbits.client;

/** Loader-neutral render-phase ordering used by {@link RenderHelper}. */
public enum RenderPhase {
    ALWAYS_ON_TOP,
    SHAPE_OUTLINES,
    TRANSLUCENT_CUSTOM_GEOMETRY
}
