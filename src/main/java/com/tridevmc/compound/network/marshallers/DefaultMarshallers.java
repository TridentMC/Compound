/*
 * Copyright 2018 - 2021 TridentMC
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

package com.tridevmc.compound.network.marshallers;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps.EntryTransformer;
import com.tridevmc.compound.network.message.MessageField;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Default marshallers, creates marshallers to handle primitives and common objects.
 */
public class DefaultMarshallers {

    public static List<MarshallerMetadata> genDefaultMarshallers() {
        List<MarshallerMetadata> out = Lists.newArrayList();

        // Register the var int/long stuff first so its less likely to be used.
        out.add(new MarshallerMetadata(new String[]{"varint"}, new StaticSimpleMarshaller<Integer>(
                DefaultMarshallers::readVarInt,
                DefaultMarshallers::writeVarInt),
                new Class[]{Integer.class, int.class}));

        out.add(new MarshallerMetadata(new String[]{"varlong"}, new StaticSimpleMarshaller<Long>(
                DefaultMarshallers::readVarLong,
                DefaultMarshallers::writeVarLong),
                new Class[]{Long.class, long.class}));

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

        out.add(new MarshallerMetadata(new String[]{"Enum", "enum"},
                new EnumMarshaller(),
                new Class[]{Enum.class}));

        out.add(new MarshallerMetadata(new String[]{"String"}, new StaticSimpleMarshaller<String>(
                DefaultMarshallers::readString,
                DefaultMarshallers::writeString),
                new Class[]{String.class}));

        out.add(new MarshallerMetadata(new String[]{"ItemStack"}, new StaticSimpleMarshaller<ItemStack>(
                DefaultMarshallers::readItemStack,
                DefaultMarshallers::writeItemStack),
                new Class[]{ItemStack.class}));

        out.add(new MarshallerMetadata(new String[]{"NBTTagCompound", "Tag", "NBT"},
                new StaticSimpleMarshaller<CompoundNBT>(
                        DefaultMarshallers::readTag,
                        DefaultMarshallers::writeTag),
                new Class[]{CompoundNBT.class}));

        out.add(new MarshallerMetadata(new String[]{"BlockPos", "Vec3i"},
                new StaticSimpleMarshaller<BlockPos>(
                        DefaultMarshallers::readBlockPos,
                        DefaultMarshallers::writeBlockPos),
                new Class[]{BlockPos.class, Vector3i.class}));


        return out;
    }

    private static void writeVarInt(ByteBuf buf, int toWrite) {
        new PacketBuffer(buf).writeVarInt(toWrite);
    }

    private static int readVarInt(ByteBuf buf) {
        return new PacketBuffer(buf).readVarInt();
    }

    private static void writeVarLong(ByteBuf buf, long toWrite) {
        new PacketBuffer(buf).writeVarLong(toWrite);
    }

    private static long readVarLong(ByteBuf buf) {
        return new PacketBuffer(buf).readVarLong();
    }

    private static void writeString(ByteBuf buf, String str) {
        new PacketBuffer(buf).writeUtf(str);
    }

    private static String readString(ByteBuf buf) {
        return new PacketBuffer(buf).readUtf(32767);
    }

    private static void writeTag(ByteBuf buf, CompoundNBT tag) {
        new PacketBuffer(buf).writeNbt(tag);
    }

    private static CompoundNBT readTag(ByteBuf buf) {
        return new PacketBuffer(buf).readNbt();
    }

    private static void writeItemStack(ByteBuf buf, ItemStack stack) {
        new PacketBuffer(buf).writeItemStack(stack, false);
    }

    private static ItemStack readItemStack(ByteBuf buf) {
        return new PacketBuffer(buf).readItem();
    }

    private static BlockPos readBlockPos(ByteBuf buf) {
        return BlockPos.of(readVarLong(buf));
    }

    private static void writeBlockPos(ByteBuf buf, BlockPos value) {
        writeVarLong(buf, value.asLong());
    }

    private static class SimpleMarshaller<T> extends Marshaller<T> {

        private final Function<ByteBuf, T> readMethod;
        private final EntryTransformer<ByteBuf, T, ByteBuf> writeMethod;

        public SimpleMarshaller(Function<ByteBuf, T> readMethod,
                                EntryTransformer<ByteBuf, T, ByteBuf> writeMethod) {
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;

            Objects.requireNonNull(readMethod);
            Objects.requireNonNull(writeMethod);
        }

        @Override
        public T readFrom(ByteBuf buf) {
            return this.readMethod.apply(buf);
        }

        @Override
        public void writeTo(ByteBuf buf, T obj) {
            this.writeMethod.transformEntry(buf, obj);
        }
    }

    private static class StaticSimpleMarshaller<T> extends Marshaller<T> {

        private final Function<ByteBuf, T> readMethod;
        private final BiConsumer<ByteBuf, T> writeMethod;

        public StaticSimpleMarshaller(Function<ByteBuf, T> readMethod,
                                      BiConsumer<ByteBuf, T> writeMethod) {
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;

            Objects.requireNonNull(readMethod);
            Objects.requireNonNull(writeMethod);
        }

        @Override
        public T readFrom(ByteBuf buf) {
            return this.readMethod.apply(buf);
        }

        @Override
        public void writeTo(ByteBuf buf, T obj) {
            this.writeMethod.accept(buf, obj);
        }
    }

    private static class EnumMarshaller<T extends Enum> extends Marshaller<T> {

        @Override
        public T readFrom(MessageField field, ByteBuf buf) {
            return (T) field.getType().getEnumConstants()[buf.readInt()];
        }

        @Override
        public T readFrom(ByteBuf buf) {
            throw new RuntimeException("Unable to read enum with no field context.");
        }

        @Override
        public void writeTo(ByteBuf buf, T obj) {
            buf.writeInt(obj.ordinal());
        }
    }


}
