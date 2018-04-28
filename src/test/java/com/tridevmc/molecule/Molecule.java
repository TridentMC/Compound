package com.tridevmc.molecule;


import com.tridevmc.compound.gui.CompoundGui;
import com.tridevmc.compound.gui.CompoundTestGui;
import com.tridevmc.compound.gui.widget.WidgetTest;
import com.tridevmc.compound.network.core.CompoundNetwork;
import com.tridevmc.molecule.init.MLBlocks;
import com.tridevmc.molecule.network.ClientTestMessage;
import com.tridevmc.molecule.network.ServerTestMessage;
import com.tridevmc.molecule.proxy.CommonProxy;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Molecule.MOD_ID,
    name = Molecule.NAME,
    version = Molecule.VERSION)
@Mod.EventBusSubscriber
public final class Molecule {

    public static final String MOD_ID = "molecule";
    public static final String NAME = "Moldule";
    public static final String VERSION = "1.12.2-0.1.0";

    public static final Logger LOG = LogManager.getLogger(Molecule.NAME);
    @SidedProxy(
        clientSide = "com.tridevmc.molecule.proxy.ClientProxy",
        serverSide = "com.tridevmc.molecule.proxy.CommonProxy")
    public static CommonProxy PROXY;
    public static MoleculeCreativeTab CREATIVE_TAB = new MoleculeCreativeTab(Molecule.NAME);
    @Mod.Instance
    public static Molecule INSTANCE;
    static CompoundGui gui;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        MLBlocks.registerBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        MLBlocks.registerItemBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        MLBlocks.registerBlockModels();
    }

    @SubscribeEvent
    public static void renderEvent(RenderGameOverlayEvent.Text event) {
        gui.drawScreen(0, 0);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        PROXY.init();

        CompoundNetwork.createNetwork("molecule", event.getAsmData());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        gui = new CompoundTestGui();
        gui.getGrid().registerWidget(new WidgetTest(), 0, 0);
    }


    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent e) {
        if (e.getEntity() instanceof EntityPlayer) {
            if (e.getEntity() instanceof EntityPlayerMP) {
                new ServerTestMessage(true).sendTo((EntityPlayerMP) e.getEntity());
            } else if (e.getEntity() instanceof EntityPlayerSP) {
                new ClientTestMessage(true).sendToServer();
            }
        }
    }

}
