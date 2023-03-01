/*
 * Copyright 2018 - 2022 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.core.reflect;

import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * A wrapper for fields that allows easier access to their values.
 */
public class WrappedField<T> {

    private final Field field;
    private final boolean isStatic;

    private WrappedField(@Nonnull Field field) {
        this.field = field;
        this.isStatic = Modifier.isStatic(field.getModifiers());
    }

    /**
     * Wraps the given field.
     *
     * @param field the real field.
     * @return a WrappedField representing the given field.
     */
    public static <T> WrappedField<T> create(@Nonnull Field field) {
        return new WrappedField<>(field);
    }

    /**
     * Finds a field matching the given information and creates a wrapper for it.
     *
     * @param clazz     the class to search within.
     * @param fieldName the field name to search.
     * @return a WrappedField representing the field that was found.
     */
    @Nullable
    public static <T> WrappedField<T> create(Class<?> clazz, String fieldName) {
        Field f = FieldUtils.getField(clazz, fieldName, true);
        return f == null ? null : new WrappedField<>(FieldUtils.getField(clazz, fieldName, true));
    }

    /**
     * Finds a field matching the given information and creates a wrapper for it.
     *
     * @param clazz      the class to search within.
     * @param fieldNames the possible names of the field.
     * @return a WrappedField representing the field that was found.
     */
    @Nullable
    public static <T> WrappedField<T> create(Class<?> clazz, String... fieldNames) {
        Field f = null;
        for (String fieldName : fieldNames) {
            if (f != null) {
                break;
            }
            f = FieldUtils.getDeclaredField(clazz, fieldName, true);
        }

        return f == null ? null : new WrappedField<>(f);
    }

    /**
     * Gets the type of the field, only use if the field is static.
     *
     * @return the type of the static field.
     */
    public T get() {
        return this.get(null);
    }

    /**
     * Sets the type on the field, only use if the field is static.
     *
     * @param value the type to set on the static field.
     */
    public void set(T value) {
        this.set(null, value);
    }

    /**
     * Gets the type of the field on the given target.
     *
     * @param target the instance to get the field type from.
     * @return the type of the field on the given target.
     */
    public T get(Object target) {
        return this.get(target, true);
    }

    /**
     * Gets the type of the field on the given target.
     *
     * @param target the instance to get the field type from.
     * @param force  whether to force access to get the type.
     * @return the type of the field on the given target.
     */
    @SuppressWarnings("unchecked") // We already know that the field type is correct.
    public T get(Object target, boolean force) {
        try {
            if (this.isStatic) {
                return (T) FieldUtils.readStaticField(this.field, force);
            } else {
                return (T) FieldUtils.readField(this.field, target, force);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Failed to read value of field %s", this.field.getName()), e);
        }
    }

    /**
     * Sets the type on the field of the target instance to the given type.
     *
     * @param target the instance to set the type on.
     * @param value  the new type of the field.
     */
    public void set(Object target, T value) {
        this.set(target, value, true);
    }

    /**
     * Sets the type of the field on the given target instance to the given type.
     *
     * @param target the instance to set the type on.
     * @param value  the new type of the field.
     * @param force  whether to force access to set the type.
     */
    public void set(Object target, T value, boolean force) {
        try {
            if (this.isStatic) {
                FieldUtils.writeStaticField(this.field, value, force);
            } else {
                FieldUtils.writeField(this.field, target, value, force);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Failed to write type of field %s", this.field.getName()), e);
        }
    }

    /**
     * Gets the declaring class of the field.
     * <p>
     * Delegates to method in Field class.
     *
     * @return the class that declared this field.
     */
    public Class<?> getDeclaringClass() {
        return this.field.getDeclaringClass();
    }

    /**
     * Gets the name of the field.
     * <p>
     * Delegates to method in Field class.
     *
     * @return the field's name.
     */
    public String getName() {
        return this.field.getName();
    }

    /**
     * Gets the type of the field.
     * <p>
     * Delegates to method in Field class.
     *
     * @return the field's type.
     */
    public Class<T> getType() {
        return (Class<T>) this.field.getType();
    }

    /**
     * Gets the type of the field with generics in tact.
     * <p>
     * Delegates to method in Field class.
     *
     * @return the field's type with generics.
     */
    public Type getGenericType() {
        return this.field.getGenericType();
    }

    /**
     * Gets the annotation of the given type and class on this field.
     * <p>
     * Delegates to method in Field class.
     *
     * @param annotationClass the class of the annotation.
     * @param <A>             the type of annotation to receive
     * @return the annotation of the given type and class.
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return this.field.getAnnotation(annotationClass);
    }

    /**
     * Gets an array of annotations matching the given type and class on this field.
     * <p>
     * Delegates to method in Field class.
     *
     * @param annotationClass the class of the desired annotations.
     * @param <A>             the type of annotation to receive.
     * @return an array of matching annotations.
     */
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        return this.field.getAnnotationsByType(annotationClass);
    }

    /**
     * Checks if this field is accessible.
     * <p>
     * Delegates to method in Field class.
     *
     * @return whether the this field is accessible or not.
     */
    public boolean isAccessible() {
        return this.field.isAccessible();
    }

    /**
     * Sets the accessibility of the field to the type specified.
     * <p>
     * Delegates to method in Field class.
     *
     * @param b the accessibility to set the field to.
     */
    public void setAccessible(boolean b) {
        this.field.setAccessible(b);
    }

    /**
     * Checks if the given annotation class is present on this field.
     * <p>
     * Delegates to method in Field class.
     *
     * @param annotationClass the class of annotation to search for.
     * @return whether an annotation matching the class was found.
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return this.field.isAnnotationPresent(annotationClass);
    }

}