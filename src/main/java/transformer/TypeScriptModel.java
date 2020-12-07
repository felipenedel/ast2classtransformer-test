package transformer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to generate typescript interfaces using cz.habarta.typescript-generator
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface TypeScriptModel {
}
