package dev.chux.gcp.crun.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

import com.google.inject.Singleton;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Target({ FIELD, PARAMETER, METHOD })
@Retention(RUNTIME)
@Singleton
public @interface MainThread {}
