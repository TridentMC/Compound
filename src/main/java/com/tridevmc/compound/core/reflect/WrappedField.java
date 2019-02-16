package com.tridevmc.compound.core.reflect;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * A wrapper for fields that allows easier access to their values.
 */
public class WrappedField<T> {

    private Field field;
    private boolean isStatic;

    private WrappedField(Field field) {
        this.field = field;
        this.isStatic = Modifier.isStatic(field.getModifiers());
    }

    /**
     * Wraps the given field.
     *
     * @param field the real field.
     * @return a WrappedField representing the given field.
     */
    public static <T> WrappedField<T> create(Field field) {
        return new WrappedField<T>(field);
    }

    /**
     * Finds a field matching the given information and creates a wrapper for it.
     *
     * @param clazz     the class to search within.
     * @param fieldName the field name to search.
     * @return a WrappedField representing the field that was found.
     */
    public static <T> WrappedField<T> create(Class clazz, String fieldName) {
        return new WrappedField<T>(FieldUtils.getField(clazz, fieldName, true));
    }

    /**
     * Finds a field matching the given information and creates a wrapper for it.
     *
     * @param clazz      the class to search within.
     * @param fieldNames the possible names of the field.
     * @return a WrappedField representing the field that was found.
     */
    public static <T> WrappedField<T> create(Class clazz, String[] fieldNames) {
        Field f = null;
        for (String fieldName : fieldNames) {
            if (f != null) {
                break;
            }
            f = FieldUtils.getDeclaredField(clazz, fieldName, true);
        }

        return new WrappedField<T>(f);
    }

    /**
     * Gets the value of the field, only use if the field is static.
     *
     * @return the value of the static field.
     */
    public T getStaticValue() {
        return this.getValue(null);
    }

    /**
     * Gets the value of the field on the given target.
     *
     * @param target the instance to get the field value from.
     * @return the value of the field on the given target.
     */
    public T getValue(Object target) {
        return this.getValue(target, true);
    }

    /**
     * Gets the value of the field on the given target.
     *
     * @param target the instance to get the field value from.
     * @param force  whether to force access to get the value.
     * @return the value of the field on the given target.
     */
    public T getValue(Object target, boolean force) {
        try {
            if (this.isStatic) {
                return (T) FieldUtils.readStaticField(this.field, force);
            } else {
                return (T) FieldUtils.readField(this.field, target, force);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Failed to read value of field %s", this.field.getName()), e);
        }
    }

    /**
     * Sets the value on the field, only use if the field is static.
     *
     * @param value the value to set on the static field.
     */
    public void setStaticValue(T value) {
        this.setValue(null, value);
    }

    /**
     * Sets the value on the field of the target instance to the given value.
     *
     * @param target the instance to set the value on.
     * @param value  the new value of the field.
     */
    public void setValue(Object target, T value) {
        this.setValue(target, value, true);
    }

    /**
     * Sets the value of the field on the given target instance to the given value.
     *
     * @param target the instance to set the value on.
     * @param value  the new value of the field.
     * @param force  whether to force access to set the value.
     */
    public void setValue(Object target, T value, boolean force) {
        try {
            if (this.isStatic) {
                FieldUtils.writeStaticField(this.field, value);
            } else {
                FieldUtils.writeField(this.field, target, value, force);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Failed to write value of field %s", this.field.getName()), e);
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
        return field.getDeclaringClass();
    }

    /**
     * Gets the name of the field.
     * <p>
     * Delegates to method in Field class.
     *
     * @return the field's name.
     */
    public String getName() {
        return field.getName();
    }

    /**
     * Gets the type of the field.
     * <p>
     * Delegates to method in Field class.
     *
     * @return the field's type.
     */
    public Class<?> getType() {
        return field.getType();
    }

    /**
     * Gets the type of the field with generics in tact.
     * <p>
     * Delegates to method in Field class.
     *
     * @return the field's type with generics.
     */
    public Type getGenericType() {
        return field.getGenericType();
    }

    /**
     * Gets the annotation of the given type and class on this field.
     * <p>
     * Delegates to method in Field class.
     *
     * @param annotationClass the class of the annotation.
     * @param <T>             the type of annotation to receive
     * @return the annotation of the given type and class.
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return field.getAnnotation(annotationClass);
    }

    /**
     * Gets an array of annotations matching the given type and class on this field.
     * <p>
     * Delegates to method in Field class.
     *
     * @param annotationClass the class of the desired annotations.
     * @param <T>             the type of annotation to receive.
     * @return an array of matching annotations.
     */
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return field.getAnnotationsByType(annotationClass);
    }

    /**
     * Checks if this field is accessible.
     * <p>
     * Delegates to method in Field class.
     *
     * @return whether the this field is accessible or not.
     */
    public boolean isAccessible() {
        return field.isAccessible();
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
        return field.isAnnotationPresent(annotationClass);
    }

    /**
     * Sets the accessibility of the field to the value specified.
     * <p>
     * Delegates to method in Field class.
     *
     * @param b the accessibility to set the field to.
     */
    public void setAccessible(boolean b) {
        field.setAccessible(b);
    }
}