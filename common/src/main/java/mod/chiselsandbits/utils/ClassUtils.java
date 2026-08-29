package mod.chiselsandbits.utils;

import org.jetbrains.annotations.Nullable;

public class ClassUtils {
    private ClassUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ClassUtils. This is a utility class");
    }

    @Nullable
    public static Class<?> getDeclaringClass(final Class<?> blkClass, final String methodName, final Class<?>... args) {
        for (Class<?> type = blkClass; type != null; type = type.getSuperclass()) {
            try {
                return type.getDeclaredMethod(methodName, args).getDeclaringClass();
            } catch (final NoSuchMethodException ignored) {
                // Keep walking: Minecraft 26.2 moved some block hooks to protected superclass methods.
            } catch (final SecurityException | LinkageError ignored) {
                return null;
            }
        }

        return null;
    }
}
