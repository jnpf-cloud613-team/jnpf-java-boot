package jnpf.model;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieLdsModelExcel {

    String fieLdsModel() default "{}";

    String type() default "mast";

}
