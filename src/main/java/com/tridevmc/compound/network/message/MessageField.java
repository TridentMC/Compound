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

package com.tridevmc.compound.network.message;

import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.network.marshallers.Marshaller;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Stores information about a field on a message object.
 * <p>
 * For internal use only.
 *
 * @param <T> the type of the message field.
 */
public class MessageField<T> {

    private final Marshaller<T> marshaller;
    private final WrappedField field;
    private final boolean isNullable;

    public MessageField(Marshaller<T> marshaller, Field field) {
        this.marshaller = marshaller;
        this.field = WrappedField.create(field);

        this.isNullable = field.isAnnotationPresent(Nullable.class);
    }

    public void writeField(Message msg, ByteBuf target) {
        // Write a boolean for null checks if the field is nullable.
        if (this.isNullable) {
            if (this.isNull(msg)) {
                target.writeBoolean(true);
                return;
            } else {
                target.writeBoolean(false);
            }
        }

        if (this.field.getType().isArray()) {
            T[] values = (T[]) this.getValue(msg);
            new FriendlyByteBuf(target).writeVarInt(values.length);
            for (T value : values) {
                this.getMarshaller().writeTo(this, target, value);
            }
        } else {
            this.getMarshaller().writeTo(this, target, (T) this.getValue(msg));
        }
    }

    public void readField(Message msg, ByteBuf source) {
        // Check if the field is nullable and null, if it is then stop reading.
        if (this.isNullable && source.readBoolean()) {
            return;
        }

        if (this.field.getType().isArray()) {
            int size = new FriendlyByteBuf(source).readVarInt();

            T[] values = (T[]) Array.newInstance(this.field.getType(), size);
            for (int i = 0; i < size; i++) {
                values[i] = this.getMarshaller().readFrom(this, source);
            }
            this.setValue(msg, values);
        } else {
            this.setValue(msg, this.getMarshaller().readFrom(this, source));
        }
    }

    public Class getType() {
        return this.getField().getType();
    }

    public void setValue(Message msg, Object value) {
        this.getField().set(msg, value);
    }

    public Object getValue(Message msg) {
        return this.getField().get(msg);
    }

    public Marshaller<T> getMarshaller() {
        return this.marshaller;
    }

    public WrappedField getField() {
        return this.field;
    }

    private boolean isNull(Message msg) {
        return this.getField().get(msg) == null;
    }

}
