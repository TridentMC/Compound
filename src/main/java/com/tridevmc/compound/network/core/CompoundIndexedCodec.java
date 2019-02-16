package com.tridevmc.compound.network.core;

//import com.tridevmc.compound.network.message.Message;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import net.minecraftforge.fml.network.simple.IndexedMessageCodec;
//
//public class CompoundIndexedCodec extends IndexedMessageCodec<Message> {
//
//    private final CompoundNetwork network;
//
//    public CompoundIndexedCodec(CompoundNetwork network) {
//        this.network = network;
//    }
//
//    @Override
//    public void encodeInto(ChannelHandlerContext ctx, Message msg, ByteBuf target) {
//        this.network.getMsgConcept(msg).toBytes(msg, target);
//    }
//
//    @Override
//    public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, Message msg) {
//        this.network.getMsgConcept(msg).fromBytes(msg, source);
//    }
//}
//