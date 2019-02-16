package com.tridevmc.molecule.network;

import com.tridevmc.compound.network.message.Message;
import com.tridevmc.molecule.Molecule;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
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
    public NBTTagCompound myTag;
    public BlockPos myPos;

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
            this.myTag = new NBTTagCompound();
            this.myTag.setFloat("Float", 4F);
            Random rand = new Random();
            this.myPos = new BlockPos(rand.nextInt(511), rand.nextInt(255), rand.nextInt(511));

            Molecule.LOG.info("Sending {}, {}", this.getClass().getName(), this.toString());
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, new MultilineRecursiveToStringStyle())
            .append("myFloat", myFloat)
            .append("myDouble", myDouble)
            .append("myByte", myByte)
            .append("myShort", myShort)
            .append("myInt", myInt)
            .append("myLong", myLong)
            .append("myBoolean", myBoolean)
            .append("myOtherBoolean", myOtherBoolean)
            .append("myChar", myChar)
            .append("myString", myString)
            .append("myItemStack", myItemStack.getItem() + ":" + myItemStack.getTag())
            .append("myTag", myTag)
            .append("myPos", myPos)
            .toString();
    }

    @Override
    public void handle(EntityPlayer player) {
        Molecule.LOG.info("Received {}, {}", this.getClass().getName(), this.toString());
    }
}
