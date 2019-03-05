package com.tridevmc.compound.network.message;

import net.minecraftforge.fml.LogicalSide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for registration of different message types, attach to a message class and provide the
 * network channel and destination for it to be auto registered.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegisteredMessage {

    /**
     * Used to determine the network channel the annotated message is made to be sent on.
     *
     * @return the name of the network channel to use.
     */
    String channel();

    /**
     * Used to determine the destination for the annotated message to be received on.
     * <p>
     * For example, CLIENT would be received and handled on the client
     *
     * @return the destination for the annotated message.
     */
    LogicalSide destination();
}
