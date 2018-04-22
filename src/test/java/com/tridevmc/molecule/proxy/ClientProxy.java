package com.tridevmc.molecule.proxy;

import com.tridevmc.molecule.Molecule;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;

@SuppressWarnings("MethodCallSideOnly")
public class ClientProxy extends CommonProxy {
	public void init() {
		super.init();
	}

	@Override
	public void registerItemRenderer(Item item, int meta, String id) {
		ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(Molecule.MOD_ID + ":" + id, "inventory"));
	}
}
