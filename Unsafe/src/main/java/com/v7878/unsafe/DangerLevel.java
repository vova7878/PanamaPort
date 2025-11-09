package com.v7878.unsafe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface DangerLevel {
    int VERY_CAREFUL = Integer.MAX_VALUE / 2;
    int ONLY_NONMOVABLE_OBJECTS = VERY_CAREFUL + 1;
    int MAX = Integer.MAX_VALUE;

    int value();
}
