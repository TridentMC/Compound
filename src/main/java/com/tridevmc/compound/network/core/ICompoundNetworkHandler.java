package com.tridevmc.compound.network.core;

import com.tridevmc.compound.network.message.Message;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public interface ICompoundNetworkHandler {

    public <M extends Message> void handle(M m, Context ctx);

    EntityPlayer getPlayer(Context ctx);

}
