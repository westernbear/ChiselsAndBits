package mod.chiselsandbits.api;

public enum ItemType {
    CHISELED_BLOCK(true),
    POSITIVE_DESIGN(true),
    NEGATIVE_DESIGN(true),
    MIRROR_DESIGN(true),
    CHISEL(false),
    BIT_BAG(false),
    CHISELED_BIT(false),
    WRENCH(false);

    public final boolean isBitAccess;

    ItemType(final boolean bitAccess) {
        isBitAccess = bitAccess;
    }
}
