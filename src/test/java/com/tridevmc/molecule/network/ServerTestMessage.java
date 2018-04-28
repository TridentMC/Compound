package com.tridevmc.molecule.network;

import com.tridevmc.compound.network.message.RegisteredMessage;
import net.minecraftforge.fml.relauncher.Side;

@RegisteredMessage(destination = Side.SERVER, networkChannel = "molecule")
public class ServerTestMessage extends TestMessage {

    public ServerTestMessage() {
    }

    public ServerTestMessage(boolean genValues) {
        super(genValues);
    }
}
