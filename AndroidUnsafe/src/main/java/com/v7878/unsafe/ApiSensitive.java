package com.v7878.unsafe;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({TYPE,
        FIELD,
        METHOD,
        PARAMETER,
        CONSTRUCTOR,
        LOCAL_VARIABLE,
        ANNOTATION_TYPE,
        PACKAGE,
        TYPE_PARAMETER,
        TYPE_USE,
        MODULE,
        RECORD_COMPONENT})
@Retention(RetentionPolicy.SOURCE)
public @interface ApiSensitive {
}
