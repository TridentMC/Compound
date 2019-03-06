package com.tridevmc.compound.config;

/**
 * Used to define custom serialization steps for objects used in configs.
 * Register with {@link RegisteredConfigObjectSerializer}
 *
 * @param <T> the type of object to serialize.
 */
public interface IConfigObjectSerializer<T> {

    /**
     * Convert the given value to a string to be de-serialized later.
     *
     * @param fieldType the type of field the value is stored in.
     * @param value     the value to convert to a string.
     * @return the value converted to a string.
     */
    String toString(Class fieldType, T value);

    /**
     * Convert the given string back into an object.
     *
     * @param fieldType the type of field the value is stored in.
     * @param value     the previously serialized object to deserialize.
     * @return the de-serialized object.
     */
    T fromString(Class fieldType, String value);

    /**
     * Determines if the given class can be serialized by this serializer.
     *
     * @param clazz the class to check against.
     * @return true if the class can be serialized by this serializer, false otherwise.
     */
    boolean accepts(Class clazz);
}
