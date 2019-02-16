package com.tridevmc.molecule;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public class MoleculeItemGroup extends ItemGroup {

    public MoleculeItemGroup(String name) {
        super(name);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(Blocks.BARRIER, 1);
    }
}
