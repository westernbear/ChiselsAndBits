package mod.chiselsandbits.client.model.data;

import org.jetbrains.annotations.Nullable;

public interface IModelData {
    @Nullable
    <T> T getData(ModelProperty<T> prop);

    @Nullable
    <T> T setData(ModelProperty<T> prop, T data);
}
