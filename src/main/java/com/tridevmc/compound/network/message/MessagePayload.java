package com.tridevmc.compound.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Small dummy class that routes our own network messages to the vanilla packet system.
 */
public class MessagePayload implements CustomPacketPayload {

    private final MessageConcept concept;
    private final Message message;

    public MessagePayload(MessageConcept concept, Message message) {
        this.concept = concept;
        this.message = message;
    }

    public void write(FriendlyByteBuf bb) {
        this.concept.toBytes(this.message, bb);
    }

    protected Message getMessage() {
        return this.message;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return this.concept.getMessageType();
    }
}
