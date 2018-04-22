package com.tridevmc.molecule;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class MoleculeCreativeTab extends CreativeTabs {
	public MoleculeCreativeTab(String name) {
		super(name);
	}

	@Override
	public ItemStack getTabIconItem() {
		return new ItemStack(Blocks.BARRIER, 1);
	}
}
