package com.tridevmc.molecule.network;

import com.tridevmc.compound.network.message.RegisteredMessage;
import net.minecraftforge.api.distmarker.Dist;

@RegisteredMessage(destination = Dist.DEDICATED_SERVER, networkChannel = "molecule:network")
public class ServerTestMessage extends TestMessage {

    public ServerTestMessage() {
    }

    public ServerTestMessage(boolean genValues) {
        super(genValues);
    }
}
