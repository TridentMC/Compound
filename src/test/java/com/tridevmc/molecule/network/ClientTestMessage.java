package com.tridevmc.molecule.network;


import com.tridevmc.compound.network.message.RegisteredMessage;
import net.minecraftforge.fml.relauncher.Side;

@RegisteredMessage(destination = Side.CLIENT, networkChannel = "molecule")
public class ClientTestMessage extends TestMessage {

    public ClientTestMessage() {
    }

    public ClientTestMessage(boolean genValues) {
        super(genValues);
    }
}
