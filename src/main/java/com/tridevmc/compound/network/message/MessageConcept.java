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

import com.tridevmc.compound.network.core.CompoundNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.fml.LogicalSide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stores information about a message for use in serializing and deserializing.
 * <p>
 * For internal use only.
 */
public class MessageConcept {

    private final CompoundNetwork network;
    private final Class<? extends Message> messageClass;
    private final LogicalSide messageSide;
    private final ArrayList<MessageField> messageFields;

    public MessageConcept(CompoundNetwork network, Class<? extends Message> messageClass,
                          ArrayList<MessageField> messageFields, LogicalSide messageSide) {
        this.network = network;
        this.messageClass = messageClass;
        this.messageSide = messageSide;
        this.messageFields = messageFields;
    }

    public LogicalSide getMessageSide() {
        return this.messageSide;
    }

    public void toBytes(Message msg, ByteBuf target) {
        List<MessageField> booleanFields = this.messageFields.stream()
                .filter(this::isFieldBoolean).sorted(
                        Comparator.comparing(o -> o.getField().getName())).collect(Collectors.toList());

        if (!booleanFields.isEmpty()) {
            byte currentByte = 0x00000000;
            int byteIndex = 0;
            for (int i = 0; i < booleanFields.size(); i++) {
                if (i == 8) {
                    byteIndex = 0;
                    currentByte = 0x00000000;
                }

                MessageField msgField = booleanFields.get(i);
                boolean fieldValue = (boolean) msgField.getValue(msg);

                if (fieldValue) {
                    currentByte |= (1 << byteIndex);
                }

                if (i + 1 == booleanFields.size() || currentByte == 7) {
                    target.writeByte(currentByte);
                }
            }
        }

        for (MessageField msgField : this.messageFields) {
            if (!this.isFieldBoolean(msgField)) {
                msgField.writeField(msg, target);
            }
        }
    }

    public <M extends Message> M fromBytes(FriendlyByteBuf source) {
        Message msg = null;
        try {
            msg = this.messageClass.newInstance();
            this.fromBytes(msg, source.capacity(source.capacity()));
        } catch (Exception e) {
            this.network.getLogger().error("Failed to create new instance of {}, caused by {}", this.messageClass.getName(), e);
        }
        return (M) msg;
    }

    public void fromBytes(Message msg, ByteBuf source) {
        List<MessageField> booleanFields = this.messageFields.stream()
                .filter(this::isFieldBoolean).sorted(
                        Comparator.comparing(o -> o.getField().getName())).collect(Collectors.toList());

        if (!booleanFields.isEmpty()) {
            int byteIndex = 0;
            byte currentByte = source.readByte();
            for (int i = 0; i < booleanFields.size(); i++) {
                if (i == 8) {
                    byteIndex = 0;
                    currentByte = source.readByte();
                }

                MessageField msgField = booleanFields.get(i);
                msgField.setValue(msg, (currentByte & (1 << byteIndex)) != 0);
            }
        }

        for (MessageField msgField : this.messageFields) {
            if (!this.isFieldBoolean(msgField)) {
                msgField.readField(msg, source);
            }
        }
    }

    private boolean isFieldBoolean(MessageField msgField) {
        return msgField.getType() == Boolean.class || msgField.getType() == boolean.class;
    }

}
