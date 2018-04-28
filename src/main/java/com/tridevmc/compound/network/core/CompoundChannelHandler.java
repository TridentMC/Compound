package com.tridevmc.compound.network.core;

import com.tridevmc.compound.network.message.Message;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Sharable
public class CompoundChannelHandler extends ChannelInboundHandlerAdapter {

    private final CompoundNetwork network;

    public CompoundChannelHandler(CompoundNetwork network) {
        this.network = network;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        boolean release = true;
        try {
            if (msg instanceof Message) {
                Side side = network.getMsgConcept((Message) msg).getMessageSide();
                INetHandler netHandler = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();

                EntityPlayer player = null;
                if (side.isClient()) {
                    player = getClientPlayer();
                } else if (side.isServer()) {
                    player = getServerPlayer(netHandler);
                }

                ((Message) msg).handle(player);
            } else {
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {
            if (release) {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        network.getLogger().error("Caught exception in CompoundChannelHandler", cause);
        super.exceptionCaught(ctx, cause);
    }

    @SideOnly(Side.CLIENT)
    private EntityPlayer getClientPlayer() {
        return net.minecraft.client.Minecraft.getMinecraft().player;
    }

    private EntityPlayer getServerPlayer(INetHandler netHandler) {
        if (netHandler instanceof NetHandlerPlayServer) {
            return ((NetHandlerPlayServer) netHandler).player;
        }

        return null;
    }
}
