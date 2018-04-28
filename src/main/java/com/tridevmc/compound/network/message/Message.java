package com.tridevmc.compound.network.message;

import com.tridevmc.compound.network.core.CompoundNetwork;
import io.netty.channel.ChannelFutureListener;
import java.util.EnumMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Base class for any Compound Message, extend this and provide an empty constructor then add any
 * fields that you want to have sent.
 *
 * To register your class as a message use @RegisteredMessage
 *
 * To specify a custom marshaller for field use @SetMarshaller
 */
public abstract class Message {

    public abstract void handle(EntityPlayer player);

    public CompoundNetwork getNetwork() {
        return CompoundNetwork.getNetworkFor(this.getClass());
    }

    /**
     * Sends this message to all clients in all dimensions.
     */
    public void sendToAll() {
        EnumMap<Side, FMLEmbeddedChannel> channels = this.getNetwork().getChannels();

        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
        channels.get(Side.SERVER).writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }


    /**
     * Sends this message to the client of the given player.
     *
     * @param player the player to send the message to.
     */
    public void sendTo(EntityPlayerMP player) {
        EnumMap<Side, FMLEmbeddedChannel> channels = this.getNetwork().getChannels();

        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        channels.get(Side.SERVER).writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * Sends this message to all clients around the given point in the given dimension.
     *
     * @param dimension the dimension the point is in.
     * @param pos the coordinates of the point.
     * @param range the range around the point that the target clients are within.
     */
    public void sendToAllAround(int dimension, BlockPos pos, double range) {
        this.sendToAllAround(new TargetPoint(dimension, pos.getX(), pos.getY(), pos.getZ(), range));
    }

    /**
     * Sends this message to all clients within range of the given target point.
     *
     * @param point the point and range that the target clients are within.
     */
    public void sendToAllAround(NetworkRegistry.TargetPoint point) {
        EnumMap<Side, FMLEmbeddedChannel> channels = this.getNetwork().getChannels();

        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(point);
        channels.get(Side.SERVER).writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }


    /**
     * Sends this message to all clients tracking the given target point.
     *
     * @param point the point that the target clients are tracking.
     */
    public void sendToAllTracking(NetworkRegistry.TargetPoint point) {
        EnumMap<Side, FMLEmbeddedChannel> channels = this.getNetwork().getChannels();

        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TRACKING_POINT);
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(point);
        channels.get(Side.SERVER).writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * Sends this message to all clients tracking the given entity.
     *
     * @param entity the entity that the target clients are tracking.
     */
    public void sendToAllTracking(Entity entity) {
        EnumMap<Side, FMLEmbeddedChannel> channels = this.getNetwork().getChannels();

        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TRACKING_ENTITY);
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(entity);
        channels.get(Side.SERVER).writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * Sends this message to all clients tracking the given position in the given dimension.
     *
     * @param dimension the dimension the point is in.
     * @param pos the coordinates of the point.
     */
    public void sendToAllTracking(int dimension, BlockPos pos) {
        this.sendToAllTracking(new TargetPoint(dimension, pos.getX(), pos.getY(), pos.getZ(), 0));
    }

    /**
     * Sends this message to the all clients in the given dimension.
     *
     * @param dimensionId the id of the dimension this message should be sent to.
     */
    public void sendToDimension(int dimensionId) {
        EnumMap<Side, FMLEmbeddedChannel> channels = this.getNetwork().getChannels();

        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.DIMENSION);
        channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(dimensionId);
        channels.get(Side.SERVER).writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * Sends this message to the server to be executed.
     */
    public void sendToServer() {
        EnumMap<Side, FMLEmbeddedChannel> channels = this.getNetwork().getChannels();

        channels.get(Side.CLIENT).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
        channels.get(Side.CLIENT).writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

}
