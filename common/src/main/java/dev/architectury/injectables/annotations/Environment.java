package dev.architectury.injectables.annotations;

import dev.architectury.utils.Env;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Compile-time environment marker used by common code. Architectury injectables
 * ships {@link ExpectPlatform} but not this annotation, so common provides a
 * compatible stub for loader-agnostic client-only members.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface Environment {
    Env value();
}
