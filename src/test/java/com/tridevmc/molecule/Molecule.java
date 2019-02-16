package com.tridevmc.molecule;


import com.tridevmc.compound.gui.CompoundGui;
import com.tridevmc.compound.gui.CompoundTestGui;
import com.tridevmc.compound.gui.widget.WidgetTest;
import com.tridevmc.compound.network.core.CompoundNetwork;
import com.tridevmc.molecule.network.ClientTestMessage;
import com.tridevmc.molecule.network.ServerTestMessage;
import com.tridevmc.molecule.proxy.ClientProxy;
import com.tridevmc.molecule.proxy.CommonProxy;
import com.tridevmc.molecule.init.MLBlocks;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("com/tridevmc/molecule")
public final class Molecule {

    public static final String MOD_ID = "com/tridevmc/molecule";
    public static final String NAME = "Molecule";
    public static final String VERSION = "1.12.2-0.1.0";

    public static final Logger LOG = LogManager.getLogger(Molecule.NAME);
    public static CommonProxy PROXY;
    public static MoleculeItemGroup CREATIVE_TAB = new MoleculeItemGroup(Molecule.NAME);
    public static Molecule INSTANCE;

    public Molecule() {
        INSTANCE = this;
        PROXY = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);

        FMLJavaModLoadingContext loadingContext = FMLJavaModLoadingContext.get();
        loadingContext.getModEventBus().addListener(this::onSetup);
    }

    private void onSetup(FMLCommonSetupEvent e) {
        PROXY.setup();

        CompoundNetwork.createNetwork(new ResourceLocation(MOD_ID, "network"), "1");

        gui = new CompoundTestGui();
        gui.getGrid().registerWidget(new WidgetTest(), 0, 0);

        MinecraftForge.EVENT_BUS.register(Molecule.class);
    }

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
