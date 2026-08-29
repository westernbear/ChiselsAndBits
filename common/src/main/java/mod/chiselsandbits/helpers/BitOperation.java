package mod.chiselsandbits.helpers;

public enum BitOperation {
    CHISEL(false, true, false, ChiselToolType.CHISEL),
    PLACE(true, false, true, ChiselToolType.BIT),
    REPLACE(true, true, false, ChiselToolType.BIT);

    final boolean bits;
    final boolean chisels;
    final boolean placementOffset;
    final ChiselToolType type;

    BitOperation(final boolean b, final boolean c, final boolean o, final ChiselToolType t) {
        bits = b;
        chisels = c;
        type = t;
        placementOffset = o;
    }

    public boolean usesBits() {
        return bits;
    }

    public boolean usesChisels() {
        return chisels;
    }

    public boolean usePlacementOffset() {
        return placementOffset;
    }

    public ChiselToolType getToolType() {
        return type;
    }
}
