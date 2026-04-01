package de.magicthings.reactivemongofilter.annotation;

import de.magicthings.reactivemongofilter.spec.EntityFilterSpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Filtered {

    Class<? extends EntityFilterSpec<?>> value();
}