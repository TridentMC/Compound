package com.tridevmc.molecule.network;

import com.tridevmc.compound.network.message.Message;
import com.tridevmc.molecule.Molecule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class TestMessage extends Message {

    public float myFloat;
    public double myDouble;
    public byte myByte;
    public short myShort;
    public int myInt;
    public long myLong;

    public boolean myBoolean;
    public boolean myOtherBoolean;
    public char myChar;
    public String myString;

    public ItemStack myItemStack;
    public CompoundTag myTag;
    public BlockPos myPos;

    public Direction myFacing;

    public TestMessage() {
        this(false);
    }

    public TestMessage(boolean genValues) {
        if (genValues) {
            this.myFloat = Float.MAX_VALUE;
            this.myDouble = Double.MAX_VALUE;
            this.myByte = Byte.MAX_VALUE;
            this.myShort = Short.MAX_VALUE;
            this.myInt = Integer.MAX_VALUE;
            this.myLong = Long.MAX_VALUE;

            this.myBoolean = true;
            this.myOtherBoolean = false;
            this.myChar = 'a';
            this.myString = "haha YES";

            this.myItemStack = Items.TRIDENT.getDefaultInstance();
            this.myTag = new CompoundTag();
            this.myTag.putFloat("Float", 4F);
            var rand = RandomSource.create();
            this.myPos = new BlockPos(rand.nextInt(511), rand.nextInt(255), rand.nextInt(511));
            this.myFacing = Direction.getRandom(rand);

            Molecule.LOG.info("Sending {}, {}", this.getClass().getName(), this.toString());
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, new MultilineRecursiveToStringStyle())
                .append("myFloat", this.myFloat)
                .append("myDouble", this.myDouble)
                .append("myByte", this.myByte)
                .append("myShort", this.myShort)
                .append("myInt", this.myInt)
                .append("myLong", this.myLong)
                .append("myBoolean", this.myBoolean)
                .append("myOtherBoolean", this.myOtherBoolean)
                .append("myChar", this.myChar)
                .append("myString", this.myString)
                .append("myItemStack", this.myItemStack.getItem() + ":" + this.myItemStack.getDamageValue())
                .append("myTag", this.myTag)
                .append("myPos", this.myPos)
                .append("myFacing", this.myFacing.toString())
                .toString();
    }

    @Override
    public void handle(Player player) {
        Molecule.LOG.info("Received {}, {}", this.getClass().getName(), this.toString());
    }

}
