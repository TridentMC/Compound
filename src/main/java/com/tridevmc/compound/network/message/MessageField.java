package com.tridevmc.compound.network.message;

import com.tridevmc.compound.core.reflect.WrappedField;
import com.tridevmc.compound.network.marshallers.Marshaller;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;

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
            if (isNull(msg)) {
                target.writeBoolean(true);
                return;
            } else {
                target.writeBoolean(false);
            }
        }

        if (field.getType().isArray()) {
            T[] values = (T[]) getValue(msg);
            new PacketBuffer(target).writeVarInt(values.length);
            for (T value : values) {
                getMarshaller().writeTo(this, target, value);
            }
        } else {
            getMarshaller().writeTo(this, target, (T) getValue(msg));
        }
    }

    public void readField(Message msg, ByteBuf source) {
        // Check if the field is nullable and null, if it is then stop reading.
        if (this.isNullable && source.readBoolean()) {
            return;
        }

        if (field.getType().isArray()) {
            int size = new PacketBuffer(source).readVarInt();

            T[] values = (T[]) Array.newInstance(field.getType(), size);
            for (int i = 0; i < size; i++) {
                values[i] = getMarshaller().readFrom(this, source);
            }
            setValue(msg, values);
        } else {
            setValue(msg, getMarshaller().readFrom(this, source));
        }
    }

    public Class getType() {
        return getField().getType();
    }

    public void setValue(Message msg, Object value) {
        getField().setValue(msg, value);
    }

    public Object getValue(Message msg) {
        return getField().getValue(msg);
    }

    public Marshaller<T> getMarshaller() {
        return marshaller;
    }

    public WrappedField getField() {
        return field;
    }

    private boolean isNull(Message msg) {
        return getField().getValue(msg) == null;
    }
}
