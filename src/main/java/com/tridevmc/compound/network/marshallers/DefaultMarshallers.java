package com.tridevmc.compound.network.marshallers;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps.EntryTransformer;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/**
 * Default marshallers, creates marshallers to handle primitives and common objects.
 */
public class DefaultMarshallers {

    public static List<MarshallerMetadata> genDefaultMarshallers() {
        List<MarshallerMetadata> out = Lists.newArrayList();

        // Register the var int/short stuff first so its less likely to be used.
        for (int i = 1; i <= 8; i++) {
            out.add(
                new MarshallerMetadata(new String[]{"varint" + i}, new VarIntMarshaller<Integer>(i),
                    new Class[]{Integer.class, int.class}));
        }

        out.add(new MarshallerMetadata(new String[]{"varshort"}, new StaticSimpleMarshaller<Short>(
            DefaultMarshallers::readVarShort,
            ByteBufUtils::writeVarShort),
            new Class[]{Short.class, short.class, Integer.class, int.class}));

        // Register all of the normal marshallers.
        out.add(new MarshallerMetadata(new String[]{"float"}, new SimpleMarshaller<Float>(
            ByteBuf::readFloat,
            ByteBuf::writeFloat),
            new Class[]{Float.class, float.class}));

        out.add(new MarshallerMetadata(new String[]{"double"}, new SimpleMarshaller<Double>(
            ByteBuf::readDouble,
            ByteBuf::writeDouble),
            new Class[]{Double.class, double.class}));

        out.add(new MarshallerMetadata(new String[]{"byte", "i8"}, new SimpleMarshaller<Byte>(
            ByteBuf::readByte,
            ByteBuf::writeByte),
            new Class[]{Byte.class, byte.class, Integer.class, int.class}));

        out.add(new MarshallerMetadata(new String[]{"short", "i16"}, new SimpleMarshaller<Short>(
            ByteBuf::readShort,
            ByteBuf::writeShort),
            new Class[]{Short.class, short.class, Integer.class, int.class}));

        out.add(new MarshallerMetadata(new String[]{"long", "i64"}, new SimpleMarshaller<Long>(
            ByteBuf::readLong,
            ByteBuf::writeLong),
            new Class[]{Long.class, long.class, Integer.class, int.class}));

        out.add(new MarshallerMetadata(new String[]{"int", "i32"}, new SimpleMarshaller<Integer>(
            ByteBuf::readInt,
            ByteBuf::writeInt
        ), new Class[]{Integer.class, int.class}));

        out.add(new MarshallerMetadata(new String[]{"boolean"}, new SimpleMarshaller<Boolean>(
            ByteBuf::readBoolean,
            ByteBuf::writeBoolean),
            new Class[]{Boolean.class, boolean.class}));

        out.add(new MarshallerMetadata(new String[]{"char"}, new SimpleMarshaller<Character>(
            ByteBuf::readChar,
            ByteBuf::writeChar
        ), new Class[]{Character.class, char.class}));

        out.add(new MarshallerMetadata(new String[]{"String"}, new StaticSimpleMarshaller<String>(
            ByteBufUtils::readUTF8String,
            ByteBufUtils::writeUTF8String),
            new Class[]{String.class}));

        out.add(
            new MarshallerMetadata(new String[]{"ItemStack"}, new StaticSimpleMarshaller<ItemStack>(
                ByteBufUtils::readItemStack,
                ByteBufUtils::writeItemStack),
                new Class[]{ItemStack.class}));

        out.add(new MarshallerMetadata(new String[]{"NBTTagCompound", "Tag", "NBT"},
            new StaticSimpleMarshaller<NBTTagCompound>(
                ByteBufUtils::readTag,
                ByteBufUtils::writeTag),
            new Class[]{NBTTagCompound.class}));

        out.add(new MarshallerMetadata(new String[]{"BlockPos", "Vec3i"},
            new StaticSimpleMarshaller<BlockPos>(
                DefaultMarshallers::readBlockPos,
                DefaultMarshallers::writeBlockPos),
            new Class[]{BlockPos.class, Vec3i.class}));

        return out;
    }

    private static short readVarShort(ByteBuf buf) {
        return (short) ByteBufUtils.readVarShort(buf);
    }

    private static BlockPos readBlockPos(ByteBuf buf) {
        return BlockPos.fromLong(buf.readLong());
    }

    private static void writeBlockPos(ByteBuf buf, BlockPos value) {
        buf.writeLong(value.toLong());
    }

    private static class SimpleMarshaller<T> extends MarshallerBase<T> {

        private Function<ByteBuf, T> readMethod;
        private EntryTransformer<ByteBuf, T, ByteBuf> writeMethod;

        public SimpleMarshaller(Function<ByteBuf, T> readMethod,
            EntryTransformer<ByteBuf, T, ByteBuf> writeMethod) {
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;

            Objects.requireNonNull(readMethod);
            Objects.requireNonNull(writeMethod);
        }

        @Override
        public T readFrom(ByteBuf buf) {
            return readMethod.apply(buf);
        }

        @Override
        public void writeTo(ByteBuf buf, T obj) {
            writeMethod.transformEntry(buf, obj);
        }
    }

    private static class StaticSimpleMarshaller<T> extends MarshallerBase<T> {

        private Function<ByteBuf, T> readMethod;
        private BiConsumer<ByteBuf, T> writeMethod;

        public StaticSimpleMarshaller(Function<ByteBuf, T> readMethod,
            BiConsumer<ByteBuf, T> writeMethod) {
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;

            Objects.requireNonNull(readMethod);
            Objects.requireNonNull(writeMethod);
        }

        @Override
        public T readFrom(ByteBuf buf) {
            return readMethod.apply(buf);
        }

        @Override
        public void writeTo(ByteBuf buf, T obj) {
            writeMethod.accept(buf, obj);
        }
    }

    private static class VarIntMarshaller<T> extends MarshallerBase<T> {

        private final int size;

        public VarIntMarshaller(int size) {
            this.size = size;
        }

        @Override
        public T readFrom(ByteBuf buf) {
            return (T) Integer.valueOf(ByteBufUtils.readVarInt(buf, size));
        }

        @Override
        public void writeTo(ByteBuf buf, T obj) {
            ByteBufUtils.writeVarInt(buf, (Integer) obj, size);
        }
    }

}
