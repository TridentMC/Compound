package com.tridevmc.compound.network.core;

import com.tridevmc.compound.network.message.Message;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class CompoundServerHandler implements ICompoundNetworkHandler {
    @Override
    public <M extends Message> void handle(M m, Context ctx) {
        m.handle(this.getPlayer(ctx));
        ctx.setPacketHandled(true);
    }

    @Override
    public PlayerEntity getPlayer(Context ctx) {
        return ctx.getSender();
    }
}
