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

import com.tridevmc.compound.network.message.MessageField;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Field;

/**
 * Base class for any data marshaller, marshallers are used for converting an object to and from
 * bytes for transport over the network.
 *
 * @param <T> The type of object this marshaller is made for.
 */
public abstract class Marshaller<T> {

    /**
     * Read a previously written object from the given ByteBuf.
     *
     * @param field the field that the data will be injected into.
     * @param buf   the buffer containing the item.
     * @return the object read from the buffer.
     */
    public T readFrom(MessageField field, ByteBuf buf) {
        return this.readFrom(buf);
    }

    /**
     * Read a previously written object from the given ByteBuf.
     *
     * @param buf the buffer containing the item.
     * @return the object read from the buffer.
     */
    public abstract T readFrom(ByteBuf buf);

    /**
     * Write the given object to the given ByteBuf for sending over the network.
     *
     * @param field the field the value is stored in.
     * @param buf   the buffer to write to.
     * @param obj   the object to write.
     */
    public void writeTo(MessageField field, ByteBuf buf, T obj) {
        this.writeTo(buf, obj);
    }

    /**
     * Write the given object to the given ByteBuf for sending over the network.
     *
     * @param buf the buffer to write to.
     * @param obj the object to write.
     */
    public abstract void writeTo(ByteBuf buf, T obj);

    /**
     * Creates a message field for the given field using this marshaller.
     *
     * @param field the field to create a MessageField for.
     * @return a MessageField for the given field using this marshaller.
     */
    public final MessageField<T> getMessageField(Field field) {
        return new MessageField<T>(this, field);
    }
}
