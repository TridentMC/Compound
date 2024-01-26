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

    @Override
    public void write(FriendlyByteBuf bb) {
        this.concept.toBytes(this.message, bb);
    }

    @Override
    public ResourceLocation id() {
        return this.concept.getMessageId();
    }

    protected Message getMessage() {
        return this.message;
    }
}
