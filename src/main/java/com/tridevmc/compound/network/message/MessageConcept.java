package com.tridevmc.compound.network.message;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Stores information about a message for use in serializing and deserializing.
 *
 * For internal use only.
 */
public class MessageConcept {

    private final Class<? extends Message> messageClass;
    private final Side messageSide;
    private final ArrayList<MessageField> messageFields;

    public MessageConcept(Class<? extends Message> messageClass,
        ArrayList<MessageField> messageFields, Side messageSide) {
        this.messageClass = messageClass;
        this.messageSide = messageSide;
        this.messageFields = messageFields;
    }

    public Side getMessageSide() {
        return messageSide;
    }

    public void toBytes(Message msg, ByteBuf target) {
        List<MessageField> booleanFields = messageFields.stream()
            .filter(mf -> mf.getType() == Boolean.class).sorted(
                Comparator.comparing(o -> o.getField().getName())).collect(Collectors.toList());

        if (!booleanFields.isEmpty()) {
            byte currentByte = 0x00000000;
            int byteIndex = 0;
            for (int i = 0; i < booleanFields.size(); i++) {
                if (i == 8) {
                    byteIndex = 0;
                    currentByte = 0x00000000;
                }

                MessageField<Boolean> msgField = booleanFields.get(i);
                Boolean fieldValue = (Boolean) msgField.getValue(msg);

                if (fieldValue) {
                    currentByte |= (1 << byteIndex);
                }

                if (i + 1 == booleanFields.size() || currentByte == 7) {
                    target.writeByte(currentByte);
                }
            }
        }

        for (MessageField msgField : messageFields) {
            if (msgField.getType() != Boolean.class) {
                msgField.writeField(msg, target);
            }
        }
    }

    public void fromBytes(Message msg, ByteBuf source) {
        List<MessageField> booleanFields = messageFields.stream()
            .filter(mf -> mf.getType() == Boolean.class).sorted(
                Comparator.comparing(o -> o.getField().getName())).collect(Collectors.toList());

        if (!booleanFields.isEmpty()) {
            int byteIndex = 0;
            byte currentByte = source.readByte();
            for (int i = 0; i < booleanFields.size(); i++) {
                if (i == 8) {
                    byteIndex = 0;
                    currentByte = source.readByte();
                }

                MessageField<Boolean> msgField = booleanFields.get(i);
                msgField.setValue(msg, (currentByte & (1 << byteIndex)) != 0);
            }
        }

        for (MessageField msgField : messageFields) {
            if (msgField.getType() != Boolean.class) {
                msgField.readField(msg, source);
            }
        }
    }

}
