package com.tridevmc.molecule.network;


import com.tridevmc.compound.network.message.RegisteredMessage;
import net.minecraftforge.api.distmarker.Dist;

@RegisteredMessage(destination = Dist.CLIENT, networkChannel = "molecule:network")
public class ClientTestMessage extends TestMessage {

    public ClientTestMessage() {
    }

    public ClientTestMessage(boolean genValues) {
        super(genValues);
    }
}
