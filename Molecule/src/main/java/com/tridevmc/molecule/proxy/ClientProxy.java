package com.tridevmc.molecule.proxy;


import com.tridevmc.molecule.ui.UIGallery;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.mojang.blaze3d.platform.InputConstants;

public class ClientProxy extends CommonProxy {

    public void setup() {
        super.setup();
        NeoForge.EVENT_BUS.addListener(this::onKey);
    }

    private void onKey(InputEvent.Key event) {
        if (event.getKey() != InputConstants.KEY_F8 || event.getAction() != InputConstants.PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (!(minecraft.gui.screen() instanceof UIGallery)) {
                minecraft.gui.setScreen(new UIGallery(minecraft.gui.screen()));
            }
        });
    }

    @Override
    public void registerItemRenderer(Item item, int meta, String id) {
        //ModelLoader.setCustomModelResourceLocation(item, meta,
        //    new ModelResourceLocation(Molecule.MOD_ID + ":" + id, "inventory"));
    }

}
