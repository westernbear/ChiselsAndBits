package mod.chiselsandbits.utils;

import org.jetbrains.annotations.Nullable;

public class ClassUtils {
    private ClassUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ClassUtils. This is a utility class");
    }

    @Nullable
    public static Class<?> getDeclaringClass(final Class<?> blkClass, final String methodName, final Class<?>... args) {
        try {
            return blkClass.getMethod(methodName, args).getDeclaringClass();
        } catch (final ReflectiveOperationException | SecurityException | LinkageError ignored) {
            return null;
        }
    }
}
