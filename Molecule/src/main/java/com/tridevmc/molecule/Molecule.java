package com.tridevmc.molecule;

import com.tridevmc.compound.config.CompoundConfig;
import com.tridevmc.compound.network.core.CompoundNetwork;
import com.tridevmc.molecule.config.MoleculeConfig;
import com.tridevmc.molecule.init.MoleculeContent;
import com.tridevmc.molecule.network.ClientTestMessage;
import com.tridevmc.molecule.network.ServerTestMessage;
import com.tridevmc.molecule.proxy.ClientProxy;
import com.tridevmc.molecule.proxy.CommonProxy;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("molecule")
public final class Molecule {

    public static final String MOD_ID = "molecule";
    public static final String NAME = "Molecule";
    public static final String VERSION = "1.12.2-0.1.0";

    public static final Logger LOG = LogManager.getLogger(Molecule.NAME);
    public static CommonProxy PROXY;
    public static Molecule INSTANCE;
    // public static MoleculeConfig CONFIG; // Temporarily disabled

    public Molecule() {
        INSTANCE = this;
        PROXY = FMLEnvironment.getDist().isClient() ? new ClientProxy() : new CommonProxy();

        ModLoadingContext loadingContext = ModLoadingContext.get();
        IEventBus eventBus = loadingContext.getActiveContainer().getEventBus();
        eventBus.addListener(this::onSetup);
        eventBus.register(MoleculeContent.class);

        // CONFIG = CompoundConfig.of(MoleculeConfig.class, ModLoadingContext.get().getActiveContainer()); // Temporarily disabled
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        var player = e.getEntity();
        if (player instanceof ServerPlayer) {
            new ServerTestMessage(true).sendTo((ServerPlayer) player);
        } else if (player instanceof AbstractClientPlayer) {
            new ClientTestMessage(true).sendToServer();
        }
    }

    private void onSetup(FMLCommonSetupEvent e) {
        PROXY.setup();
        NeoForge.EVENT_BUS.register(Molecule.class);
        CompoundNetwork.createNetwork(ModLoadingContext.get().getActiveContainer(), "molecule");
    }

}
