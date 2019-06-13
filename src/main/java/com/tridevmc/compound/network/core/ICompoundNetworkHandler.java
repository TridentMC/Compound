package com.tridevmc.compound.network.core;

import com.tridevmc.compound.network.message.Message;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public interface ICompoundNetworkHandler {

    <M extends Message> void handle(M m, Context ctx);

    PlayerEntity getPlayer(Context ctx);

}
