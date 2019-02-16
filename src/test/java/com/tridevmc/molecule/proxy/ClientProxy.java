package com.tridevmc.molecule.proxy;

import net.minecraft.item.Item;

public class ClientProxy extends CommonProxy {

    public void setup() {
        super.setup();
    }

    @Override
    public void registerItemRenderer(Item item, int meta, String id) {
        //ModelLoader.setCustomModelResourceLocation(item, meta,
        //    new ModelResourceLocation(Molecule.MOD_ID + ":" + id, "inventory"));
    }
}
