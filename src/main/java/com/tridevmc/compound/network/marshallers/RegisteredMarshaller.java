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

package com.tridevmc.compound.network.marshallers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for marking a marshaller for registration, required for a marshaller to function.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegisteredMarshaller {

    /**
     * The ids used for identifying the annotated marshaller, used in @SetMarshaller
     *
     * @return the ids of the annotated marshaller class.
     */
    String[] ids();

    /**
     * Used to determine the network channel the annotated marshaller is made to be registered on.
     *
     * @return the name of the network channel to use.
     */
    String channel();

    /**
     * Used to determine the priority at which this marshaller will be selected for a given field.
     *
     * @return The priority for this marshaller to be used.
     */
    EnumMarshallerPriority priority() default EnumMarshallerPriority.NORMAL;

    /**
     * An array of accepted object types that the annotated marshaller can marshall. Used to
     * automatically find a marshaller for a field in a message.
     *
     * @return an array of accepted classes that match the annotated marshaller.
     */
    Class[] acceptedTypes();
}
