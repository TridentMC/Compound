/*
 * Copyright 2018 - 2024 TridentMC
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * A wrapper for fields that allows easier access to their values.
 */
public class WrappedField<T> {

    private final Field field;
    private final VarHandle varHandle;
    private final boolean isStatic;
    private final boolean isFinal;

    private WrappedField(Field field, VarHandle varHandle, boolean isStatic, boolean isFinal) {
        this.field = field;
        this.varHandle = varHandle;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
    }


    /**
     * Wraps the given field.
     *
     * @param field the real field.
     * @return a WrappedField representing the given field.
     */
    public static <T> WrappedField<T> create(@Nonnull Field field) {
        return create(field.getDeclaringClass(), field.getName());
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
        try {
            var lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            var field = clazz.getDeclaredField(fieldName);
            var isStatic = Modifier.isStatic(field.getModifiers());
            var isFinal = Modifier.isFinal(field.getModifiers());
            var varHandle = lookup.unreflectVarHandle(field);
            if (isFinal) {
                field.setAccessible(true);
            }
            return new WrappedField<>(field, varHandle, isStatic, isFinal);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
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
        for (String fieldName : fieldNames) {
            var field = create(clazz, fieldName);
            if (field != null) {
                return (WrappedField<T>) field;
            }
        }
        return null;
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
    @SuppressWarnings("unchecked") // We already know that the field type is correct.
    public T get(Object target) {
        if (this.isStatic) {
            return (T) this.varHandle.get();
        } else {
            return (T) this.varHandle.get(target);
        }
    }

    /**
     * Sets the type of the field on the given target instance to the given type.
     *
     * @param target the instance to set the type on.
     * @param value  the new type of the field.
     */
    public void set(Object target, T value) {
        if (this.isFinal) {
            try {
                this.field.set(target, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to set final field: " + this.field.getName(), e);
            }
        } else {
            if (this.isStatic) {
                this.varHandle.set(value);
            } else {
                this.varHandle.set(target, value);
            }
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