/*
 * Copyright 2018 - 2024 TridentMC
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
