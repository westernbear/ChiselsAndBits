package mod.chiselsandbits.client.model.data;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ModelDataMap implements IModelData {
    private final Map<ModelProperty<?>, Object> backingMap = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getData(ModelProperty<T> prop) {
        return (T) backingMap.get(prop);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T setData(ModelProperty<T> prop, T data) {
        return (T) backingMap.put(prop, data);
    }
}
