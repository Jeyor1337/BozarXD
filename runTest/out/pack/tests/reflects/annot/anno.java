/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.annot;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value=RetentionPolicy.RUNTIME)
public @interface anno {
    public String val2() default "yes";

    public String val() default "yes";
}

