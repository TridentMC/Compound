package com.tridevmc.compound.network.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tridevmc.compound.network.marshallers.DefaultMarshallers;
import com.tridevmc.compound.network.marshallers.EnumMarshallerPriority;
import com.tridevmc.compound.network.marshallers.MarshallerBase;
import com.tridevmc.compound.network.marshallers.MarshallerMetadata;
import com.tridevmc.compound.network.marshallers.RegisteredMarshaller;
import com.tridevmc.compound.network.marshallers.SetMarshaller;
import com.tridevmc.compound.network.message.Message;
import com.tridevmc.compound.network.message.MessageConcept;
import com.tridevmc.compound.network.message.MessageField;
import com.tridevmc.compound.network.message.RegisteredMessage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation.EnumHolder;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * CompoundNetwork is used for the creation and management of networks.
 *
 * Use createNetwork to create and register a network for a given channel.
 *
 * Use @RegisteredMessage and @RegisteredMarshaller for registration of implementations.
 */
public class CompoundNetwork {

    private static final Map<Class<? extends Message>, CompoundNetwork> NETWORKS = Maps.newHashMap();

    private final Logger logger;
    private final String name;
    private EnumMap<Side, FMLEmbeddedChannel> channels;
    private CompoundIndexedCodec codec;

    private Map<Class<? extends Message>, MessageConcept> messageConcepts;
    private Map<String, MarshallerBase> marshallers;
    private Map<Class, String> marshallerIds;
    private CompoundChannelHandler universalChannelHandler;

    private CompoundNetwork(String name) {
        this.name = name;
        this.codec = new CompoundIndexedCodec(this);
        this.channels = NetworkRegistry.INSTANCE.newChannel(name, codec);
        this.universalChannelHandler = new CompoundChannelHandler(this);

        this.messageConcepts = Maps.newHashMap();
        this.marshallers = Maps.newHashMap();
        this.marshallerIds = Maps.newHashMap();

        this.logger = LogManager.getLogger("CompoundNetwork-" + name);
    }

    /**
     * Create a network with the given name with messages and marshallers loaded from the given data
     * table.
     *
     * @param name the name to use for the network.
     * @param dataTable an ASMDataTable that can be used for locating registered marshallers and
     * messages.
     * @return the created network instance.
     */
    public static CompoundNetwork createNetwork(String name, ASMDataTable dataTable) {
        try {
            CompoundNetwork network = new CompoundNetwork(name);
            network.loadDefaultMarshallers();
            network.discoverMarshallers(dataTable);
            network.discoverMessages(dataTable);
            return network;
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                "Failed to create a CompoundNetwork with name %s",
                name),
                e);
        }
    }

    /**
     * Finds the network that the given message class is registered to.
     *
     * @param msg the class of the registered message.
     * @return the network that the message class is registered to.
     */
    public static CompoundNetwork getNetworkFor(Class<? extends Message> msg) {
        return NETWORKS.getOrDefault(msg, null);
    }

    private void loadDefaultMarshallers() {
        List<MarshallerMetadata> marshallerMetadata = DefaultMarshallers.genDefaultMarshallers();

        for (MarshallerMetadata marshaller : marshallerMetadata) {
            for (String id : marshaller.ids) {
                this.marshallers.put(id, marshaller.marshaller);
            }
            for (Class acceptedType : marshaller.acceptedTypes) {
                this.marshallerIds.put(acceptedType, marshaller.ids[0]);
            }
        }
    }

    private void discoverMarshallers(ASMDataTable dataTable) throws Exception {
        Set<ASMData> registeredMarshallers = dataTable.getAll(RegisteredMarshaller.class.getName());
        ArrayList<ASMData> applicableMarshallers = Lists.newArrayList();
        for (ASMData registeredMarshaller : registeredMarshallers) {
            Map<String, Object> annotationInfo = registeredMarshaller.getAnnotationInfo();

            String networkChannel = (String) annotationInfo.get("networkChannel");
            if (Objects.equals(networkChannel, this.name)) {
                applicableMarshallers.add(registeredMarshaller);
            }
        }

        applicableMarshallers.sort(Comparator.comparingInt(
            o -> EnumMarshallerPriority
                .valueOf(((EnumHolder) o.getAnnotationInfo().get("priority")).getValue())
                .getRank()));

        for (ASMData applicableMarshaller : applicableMarshallers) {
            MarshallerBase marshaller = null;

            try {
                marshaller = (MarshallerBase) Class.forName(applicableMarshaller.getClassName())
                    .newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(String.format(
                    "Unable to find class: \"%s\" for registered marshaller.",
                    applicableMarshaller.getClassName()),
                    e);
            } catch (ClassCastException e) {
                throw new RuntimeException(String.format(
                    "Class: \"%s\" annotated with RegisteredMarshaller does not extend MarshallerBase.",
                    applicableMarshaller.getClassName()),
                    e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format(
                    "Failed to instantiate %s, is there an empty constructor?",
                    applicableMarshaller.getClassName()),
                    e);
            } catch (InstantiationException e) {
                throw new RuntimeException(String.format(
                    "Failed to instantiate %s",
                    applicableMarshaller.getClassName()),
                    e);
            }

            Map<String, Object> annotationInfo = applicableMarshaller.getAnnotationInfo();
            String[] ids = (String[]) annotationInfo.get("ids");
            Class[] acceptedTypes = (Class[]) annotationInfo.get("acceptedTypes");

            for (String id : ids) {
                this.marshallers.put(id, marshaller);
            }

            for (Class acceptedType : acceptedTypes) {
                this.marshallerIds.put(acceptedType, ids[0]);
            }
        }
    }

    private void discoverMessages(ASMDataTable dataTable) throws Exception {
        Set<ASMData> registeredMessages = dataTable.getAll(RegisteredMessage.class.getName());
        int currentDiscriminator = 0;
        for (ASMData registeredMessage : registeredMessages) {
            Map<String, Object> annotationInfo = registeredMessage.getAnnotationInfo();

            String networkChannel = (String) annotationInfo.get("networkChannel");

            if (Objects.equals(networkChannel, this.name)) {
                // Found a message that can be registered for this network instance.
                EnumHolder destinationHolder = (EnumHolder) annotationInfo.get("destination");
                Side destination = Side.valueOf(destinationHolder.getValue());
                Class<? extends Message> msgClass = null;
                try {
                    msgClass = (Class<? extends Message>) Class
                        .forName(registeredMessage.getClassName());
                    msgClass.getConstructor();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(String.format(
                        "Unable to find class: %s for registered message.",
                        registeredMessage.getClassName()),
                        e);
                } catch (ClassCastException e) {
                    throw new RuntimeException(String.format(
                        "Class \"%s\" annotated with RegisteredMessage does not extend Message.",
                        registeredMessage.getClassName()),
                        e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(String.format(
                        "Class \"%s\" does not have an empty constructor available, this is required for networking.",
                        registeredMessage.getClassName()),
                        e);
                }

                createConcept(msgClass, destination);
                registerMessage(msgClass, destination, currentDiscriminator);
                currentDiscriminator++;
            }
        }
    }

    private void createConcept(Class<? extends Message> msgClass, Side destination) {
        List<Field> usableFields = FieldUtils.getAllFieldsList(msgClass).stream().filter(field -> {
            Class fieldDeclarer = field.getDeclaringClass();
            return !fieldDeclarer.equals(Message.class) && !fieldDeclarer.equals(Object.class);
        }).collect(Collectors.toList());

        List<MessageField> messageFields = usableFields.stream().map(field -> {
            String marshallerId = this.getMarshallerIdFor(field);
            return marshallers.get(marshallerId).getMessageField(field);
        }).collect(Collectors.toList());

        MessageConcept msgConcept = new MessageConcept(msgClass, new ArrayList<>(messageFields),
            destination);
        messageConcepts.put(msgClass, msgConcept);
    }

    private String getMarshallerIdFor(Field field) {
        if (field.isAnnotationPresent(SetMarshaller.class)) {
            return field.getAnnotation(SetMarshaller.class).marshallerId();
        }
        Class fieldClass = field.getType();
        String marshallerId = this.marshallerIds.getOrDefault(fieldClass, null);
        if (marshallerId == null) {
            Optional<Class> matchingClass = this.marshallerIds.keySet().stream()
                .filter(c -> c.isAssignableFrom(fieldClass)).findFirst();

            if (matchingClass.isPresent()) {
                marshallerId = this.marshallerIds.get(matchingClass.get());
            } else {
                throw new RuntimeException(
                    "Unable to find marshaller id for " + fieldClass.getName());
            }
        }

        return marshallerId;
    }

    private void registerMessage(Class<? extends Message> msgClass, Side side, int discriminator) {
        this.codec.addDiscriminator(discriminator, msgClass);

        FMLEmbeddedChannel channel = channels.get(side);
        String type = channel.findChannelHandlerNameForType(this.codec.getClass());
        String name = msgClass.getName() + ":" + side.name();

        channels.get(side).pipeline().addAfter(type, name, this.universalChannelHandler);
        NETWORKS.put(msgClass, this);
    }

    public Logger getLogger() {
        return logger;
    }

    public MessageConcept getMsgConcept(Message msg) {
        return this.messageConcepts.get(msg.getClass());
    }

    public EnumMap<Side, FMLEmbeddedChannel> getChannels() {
        return channels;
    }
}
