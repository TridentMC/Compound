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

import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * A wrapper for methods that allows easier invocation.
 */
public class WrappedMethod<T> {

    private Method method;

    private WrappedMethod(Method method) {
        this.method = method;
    }

    /**
     * Wraps the given method.
     *
     * @param method the real method.
     * @return A WrappedMethod representing the given method.
     */
    public static <T> WrappedMethod<T> create(Method method) {
        return new WrappedMethod<>(method);
    }

    /**
     * Finds a method matching the given information and creates a wrapper for it.
     *
     * @param clazz          the class to search within.
     * @param methodName     the method name to search for.
     * @param parameterTypes the parameters of the desired method.
     * @return a WrappedMethod representing the method that was found.
     */
    public static <T> WrappedMethod<T> create(Class clazz, String methodName, Class<?>... parameterTypes) {
        return new WrappedMethod<>(MethodUtils.getMatchingMethod(clazz, methodName, parameterTypes));
    }

    /**
     * Finds a method matching the given information and creates a wrapper for it.
     *
     * @param clazz      the class to search within.
     * @param methodName the method name to search for.
     * @return a WrappedMethod representing the method that was found.
     */
    public static <T> WrappedMethod<T> create(Class clazz, String methodName) {
        return new WrappedMethod<>(MethodUtils.getMatchingMethod(clazz, methodName));
    }

    /**
     * Finds a method matching the given information and creates a wrapper for it.
     *
     * @param clazz          the class to search within.
     * @param methodNames    the method names to search for.
     * @param parameterTypes the parameters of the desired method.
     * @return a WrappedMethod representing the method that was found.
     */
    public static <T> WrappedMethod<T> create(Class clazz, String[] methodNames, Class<?>... parameterTypes) {
        Method m = null;
        for (String methodName : methodNames) {
            if (m != null) {
                break;
            }

            m = MethodUtils.getMatchingMethod(clazz, methodName, parameterTypes);
        }

        return new WrappedMethod<>(m);
    }

    /**
     * Finds a method matching the given information and creates a wrapper for it.
     *
     * @param clazz       the class to search within.
     * @param methodNames the method names to search for.
     * @return a WrappedMethod representing the method that was found.
     */
    public static <T> WrappedMethod<T> create(Class clazz, String... methodNames) {
        return create(clazz, methodNames, new Class<?>[0]);
    }

    /**
     * Invokes the method, only use if the method is static.
     *
     * @return the invocation result, or null if the method has none.
     */
    public T invoke() {
        return this.invoke(new Object[0]);
    }

    /**
     * Invokes the method with the given arguments, only use if the method is static.
     *
     * @param args the arguments to use when invoking the method.
     * @return the invocation result, or null if the method has none.
     */
    public T invoke(Object... args) {
        return this.invoke(null, true, args);
    }

    /**
     * Invokes the method on the given instance with the given arguments with optional force.
     *
     * @param instance    the instance to invoke the method on.
     * @param forceAccess whether to force access to invoke the method.
     * @param args        the arguments to pass to the method.
     * @return the invocation result, or null if the method has none.
     */
    public T invoke(Object instance, boolean forceAccess, Object... args) {
        try {
            if (forceAccess)
                this.method.setAccessible(true);
            return (T) this.method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to invoke method %s", this.getName()), e);
        }
    }

    /**
     * Gets the declaring class of the method.
     * Delegates to method in Method class.
     *
     * @return the class that declared this method.
     */
    public Class<?> getDeclaringClass() {
        return this.method.getDeclaringClass();
    }

    /**
     * Gets the name of the method.
     * Delegates to method in Method class.
     *
     * @return the name of the method.
     */
    public String getName() {
        return this.method.getName();
    }

    /**
     * Gets the return type of the method.
     * Delegates to method in Method class.
     *
     * @return the return type of the method.
     */
    public Class<?> getReturnType() {
        return this.method.getReturnType();
    }

    /**
     * Gets the generic return type of the method.
     * Delegates to method in Method class.
     *
     * @return the generic return type of the method.
     */
    public Type getGenericReturnType() {
        return this.method.getGenericReturnType();
    }

    /**
     * Gets the annotation of the given type that is on the method.
     * Delegates to method in Method class.
     *
     * @param clazz the annotation type.
     * @return the annotation.
     */
    public <A extends Annotation> A getAnnotation(Class<A> clazz) {
        return this.method.getAnnotation(clazz);
    }

    /**
     * Gets the parameters of the method.
     * Delegates to method in Method class.
     *
     * @return the parameters of the method.
     */
    public Parameter[] getParameters() {
        return this.method.getParameters();
    }

    /**
     * Gets the annotations matching the given type that are on the method.
     * Delegates to method in Method class.
     *
     * @param clazz the type of annotation.
     * @return an array of matching annotations on the method.
     */
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> clazz) {
        return this.method.getAnnotationsByType(clazz);
    }

    /**
     * Checks if the method is accessible.
     * Delegates to method in Method class.
     *
     * @return whether the method is accessible or not.
     */
    public boolean isAccessible() {
        return this.method.isAccessible();
    }

    /**
     * Sets the accessibility of the method to the value specified.
     * Delegates to method in Method class.
     *
     * @param b the accessibility to set the method to.
     */
    public void setAccessible(boolean b) {
        this.method.setAccessible(b);
    }

    /**
     * Checks if the given annotation is present on the method.
     * Delegates to method in Method class.
     *
     * @param clazz the annotation to check for.
     * @return whether an annotation matching the class was found.
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> clazz) {
        return this.method.isAnnotationPresent(clazz);
    }
}
