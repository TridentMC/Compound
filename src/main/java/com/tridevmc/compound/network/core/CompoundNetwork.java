package com.tridevmc.compound.network.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tridevmc.compound.network.marshallers.*;
import com.tridevmc.compound.network.message.Message;
import com.tridevmc.compound.network.message.MessageConcept;
import com.tridevmc.compound.network.message.MessageField;
import com.tridevmc.compound.network.message.RegisteredMessage;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CompoundNetwork is used for the creation and management of networks.
 * <p>
 * Use createNetwork to create and register a network for a given channel.
 * <p>
 * Use @RegisteredMessage and @RegisteredMarshaller for registration of implementations.
 */
public class CompoundNetwork {

    private static final Map<Class<? extends Message>, CompoundNetwork> NETWORKS = Maps.newHashMap();

    private final Logger logger;
    private final String name;
    private final SimpleChannel networkChannel;

    private Map<Class<? extends Message>, MessageConcept> messageConcepts;
    private Map<String, MarshallerBase> marshallers;
    private Map<Class, String> marshallerIds;
    private Map<Dist, ICompoundNetworkHandler> handlers;


    private CompoundNetwork(ResourceLocation name, String version) {
        this.name = name.toString();
        this.networkChannel = NetworkRegistry.ChannelBuilder.named(name)
                .clientAcceptedVersions((v) -> true)
                .serverAcceptedVersions((v) -> true)
                .networkProtocolVersion(() -> version)
                .simpleChannel();
        this.messageConcepts = Maps.newHashMap();
        this.marshallers = Maps.newHashMap();
        this.marshallerIds = Maps.newHashMap();
        this.handlers = Maps.newHashMap();
        this.handlers.put(Dist.CLIENT, new CompoundClientHandler());
        this.handlers.put(Dist.DEDICATED_SERVER, new CompoundServerHandler());

        this.logger = LogManager.getLogger("CompoundNetwork-" + name);
    }

    private <T extends NetworkEvent> void onNetworkEvent(T packet) {
        final NetworkEvent.Context wrappedContext = packet.getSource().get();
        final PacketBuffer payload = packet.getPayload();
        ResourceLocation targetNetworkReceiver = null;
        PacketBuffer data = null;
        if (payload != null) {
            targetNetworkReceiver = payload.readResourceLocation();
            final int payloadLength = payload.readVarInt();
            data = new PacketBuffer(payload.readBytes(payloadLength));
        }

    }

    /**
     * Create a network with the given name with messages and marshallers loaded from the given data
     * table.
     *
     * @param name    the name to use for the network.
     * @param version the version to use for the network.
     * @return the created network instance.
     */
    public static CompoundNetwork createNetwork(ResourceLocation name, String version) {
        try {
            CompoundNetwork network = new CompoundNetwork(name, version);
            network.loadDefaultMarshallers();
            network.discoverMarshallers();
            network.discoverMessages();
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

    private void discoverMarshallers() {
        List<ModFileScanData.AnnotationData> applicableMarshallers = getAnnotationDataOfType(RegisteredMarshaller.class);

        applicableMarshallers.sort(Comparator.comparingInt(
                o -> EnumMarshallerPriority
                        .valueOf(((ModAnnotation.EnumHolder) o.getAnnotationData().get("priority")).getValue())
                        .getRank()));

        for (ModFileScanData.AnnotationData applicableMarshaller : applicableMarshallers) {
            MarshallerBase marshaller = null;

            try {
                marshaller = (MarshallerBase) Class.forName(applicableMarshaller.getMemberName())
                        .newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(String.format(
                        "Unable to find class: \"%s\" for registered marshaller.",
                        applicableMarshaller.getMemberName()),
                        e);
            } catch (ClassCastException e) {
                throw new RuntimeException(String.format(
                        "Class: \"%s\" annotated with RegisteredMarshaller does not extend MarshallerBase.",
                        applicableMarshaller.getMemberName()),
                        e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format(
                        "Failed to instantiate %s, is there an empty constructor?",
                        applicableMarshaller.getMemberName()),
                        e);
            } catch (InstantiationException e) {
                throw new RuntimeException(String.format(
                        "Failed to instantiate %s",
                        applicableMarshaller.getMemberName()),
                        e);
            }

            Map<String, Object> annotationInfo = applicableMarshaller.getAnnotationData();
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

    private void discoverMessages() {
        List<ModFileScanData.AnnotationData> applicableMessages = getAnnotationDataOfType(RegisteredMessage.class);

        int currentDiscriminator = 0;
        for (ModFileScanData.AnnotationData registeredMessage : applicableMessages) {
            Map<String, Object> annotationInfo = registeredMessage.getAnnotationData();

            String networkChannel = (String) annotationInfo.get("networkChannel");

            if (Objects.equals(networkChannel, this.name)) {
                // Found a message that can be registered for this network instance.
                ModAnnotation.EnumHolder destinationHolder = (ModAnnotation.EnumHolder) annotationInfo.get("destination");
                Dist destination = Dist.valueOf(destinationHolder.getValue());
                Class<? extends Message> msgClass = null;
                try {
                    msgClass = (Class<? extends Message>) Class
                            .forName(registeredMessage.getMemberName());
                    msgClass.getConstructor();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(String.format(
                            "Unable to find class: %s for registered message.",
                            registeredMessage.getMemberName()),
                            e);
                } catch (ClassCastException e) {
                    throw new RuntimeException(String.format(
                            "Class \"%s\" annotated with RegisteredMessage does not extend Message.",
                            registeredMessage.getMemberName()),
                            e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(String.format(
                            "Class \"%s\" does not have an empty constructor available, this is required for networking.",
                            registeredMessage.getMemberName()),
                            e);
                }

                createConcept(msgClass, destination);
                registerMessage(msgClass, destination, currentDiscriminator);
                currentDiscriminator++;
            }
        }
    }

    private List<ModFileScanData.AnnotationData> getAnnotationDataOfType(Class annotation) {
        List<ModFileScanData> modScanData = ModList.get().getAllScanData();
        ArrayList<ModFileScanData.AnnotationData> out = Lists.newArrayList();
        String annotationName = annotation.getName();

        modScanData.stream().forEach((m) -> m.getAnnotations().stream().filter(a -> Objects.equals(a.getAnnotationType().getClassName(), annotationName)).forEach(a -> {
            Map<String, Object> annotationInfo = a.getAnnotationData();

            String networkChannel = (String) annotationInfo.get("networkChannel");
            if (Objects.equals(networkChannel, this.name)) {
                out.add(a);
            }
        }));

        return out;
    }

    private void createConcept(Class<? extends Message> msgClass, Dist destination) {
        List<Field> usableFields = FieldUtils.getAllFieldsList(msgClass).stream().filter(field -> {
            Class fieldDeclarer = field.getDeclaringClass();
            return !fieldDeclarer.equals(Message.class) && !fieldDeclarer.equals(Object.class);
        }).collect(Collectors.toList());

        List<MessageField> messageFields = usableFields.stream().map(field -> {
            String marshallerId = this.getMarshallerIdFor(field);
            return marshallers.get(marshallerId).getMessageField(field);
        }).collect(Collectors.toList());

        MessageConcept msgConcept = new MessageConcept(this, msgClass, new ArrayList<>(messageFields), destination);
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

    private <M extends Message> void registerMessage(Class<M> msgClass, Dist side, int discriminator) {
        ICompoundNetworkHandler handler = this.handlers.get(side);
        this.networkChannel.messageBuilder(msgClass, discriminator)
                .encoder(getMsgConcept(msgClass)::toBytes)
                .decoder(getMsgConcept(msgClass)::fromBytes)
                .consumer((m, ctx) -> handler.handle(m, ctx.get()))
                .add();

        NETWORKS.put(msgClass, this);
    }


    public Logger getLogger() {
        return logger;
    }

    public SimpleChannel getNetworkChannel() {
        return networkChannel;
    }

    public MessageConcept getMsgConcept(Message msg) {
        return this.messageConcepts.get(msg.getClass());
    }

    public MessageConcept getMsgConcept(Class<? extends Message> msgClass) {
        return this.messageConcepts.get(msgClass);
    }


}
