package com.tridevmc.compound.network.core;

import com.tridevmc.compound.network.message.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.network.NetworkEvent;

public class CompoundClientHandler implements ICompoundNetworkHandler {
    @Override
    public <M extends Message> void handle(M m, NetworkEvent.Context ctx) {
        m.handle(this.getPlayer(ctx));
    }

    @Override
    public EntityPlayer getPlayer(NetworkEvent.Context ctx) {
        return Minecraft.getInstance().player;
    }
}
