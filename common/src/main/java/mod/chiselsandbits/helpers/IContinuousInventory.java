package mod.chiselsandbits.helpers;

public interface IContinuousInventory {

    boolean useItem(int blockId);

    void fail(int blockId);

    boolean isValid();

    IItemInInventory getItem(int blockId);
}
