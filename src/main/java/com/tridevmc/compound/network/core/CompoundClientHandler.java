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

package com.tridevmc.compound.network.core;

import com.tridevmc.compound.network.message.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;


public class CompoundClientHandler implements ICompoundNetworkHandler {
    @Override
    public <M extends Message> void handle(M m, NetworkEvent.Context ctx) {
        m.handle(this.getPlayer(ctx));
        ctx.setPacketHandled(true);
    }

    @Override
    public Player getPlayer(NetworkEvent.Context ctx) {
        return Minecraft.getInstance().player;
    }
}
