package mod.chiselsandbits.client.model.baked;

public abstract class BaseBakedBlockModel extends BaseBakedPerspectiveModel implements DataAwareBakedModel {

    @Override
    public final boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public final boolean isGui3d() {
        return true;
    }

    @Override
    public final boolean isCustomRenderer() {
        return false;
    }

    @Override
    public LegacyItemOverrides getOverrides() {
        return LegacyItemOverrides.EMPTY;
    }
}
