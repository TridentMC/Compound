package com.tridevmc.compound.network.message;

import com.tridevmc.compound.network.core.CompoundNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;


/**
 * Base class for any Compound Message, extend this and provide an empty constructor then add any
 * fields that you want to have sent.
 * <p>
 * To register your class as a message use @RegisteredMessage
 * <p>
 * To specify a custom marshaller for field use @SetMarshaller
 */
public abstract class Message {

    private CompoundNetwork network;

    public abstract void handle(@Nullable PlayerEntity player);

    @Nonnull
    public CompoundNetwork getNetwork() {
        if (this.network == null) {
            this.network = CompoundNetwork.getNetworkFor(this.getClass());
        }
        return this.network;
    }

    /**
     * Sends this message to all clients in all dimensions.
     */
    public void sendToAll() {
        this.sendToMatching((e) -> true);
    }

    /**
     * Sends this message to all clients matching the given predicate.
     *
     * @param playerPredicate the predicate that determines if the player should receive the packet.
     */
    public void sendToMatching(@Nonnull Predicate<ServerPlayerEntity> playerPredicate) {
        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        server.getPlayerList().getPlayers().stream()
                .filter(playerPredicate)
                .forEach(this::sendTo);
    }

    /**
     * Sends this message to the client of the given player.
     *
     * @param player the player to send the message to.
     */
    public void sendTo(@Nonnull ServerPlayerEntity player) {
        this.getNetwork().getNetworkChannel().sendTo(this, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    /**
     * Sends this message to all clients around the given point in the given dimension.
     *
     * @param dimension the dimension the point is in.
     * @param pos       the coordinates of the point.
     * @param range     the range around the point that the target clients are within.
     */
    public void sendToAllAround(@Nonnull RegistryKey<World> dimension, @Nonnull BlockPos pos, double range) {
        PacketDistributor.TargetPoint target = new PacketDistributor.TargetPoint(pos.getX(), pos.getY(), pos.getZ(), range, dimension);
        this.getNetwork().getNetworkChannel().send(PacketDistributor.NEAR.with(() -> target), this);
    }

    /**
     * Sends this message to all clients tracking the given entity.
     *
     * @param entity the entity that the target clients are tracking.
     */
    public void sendToAllTracking(@Nonnull Entity entity) {
        this.getNetwork().getNetworkChannel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), this);
    }

    /**
     * Sends this message to all clients tracking the given tile entity.
     *
     * @param tile the tile entity that the target clients are tracking.
     */
    public void sendToAllTracking(@Nonnull TileEntity tile) {
        this.sendToAllTracking(tile.getWorld().getChunkAt(tile.getPos()));
    }

    /**
     * Sends this message to all clients tracking the given position in the given dimension.
     *
     * @param dimension the dimension the point is in.
     * @param pos       the coordinates of the point.
     */
    public void sendToAllTracking(@Nonnull RegistryKey<World> dimension, @Nonnull BlockPos pos) {
        MinecraftServer currentServer = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        // Dont force load the world because if we have to load the world then nobody is tracking this to begin with.
        ServerWorld world = currentServer.getWorld(dimension);
        if (world != null) {
            Chunk chunk = world.getChunkAt(pos);
            this.sendToAllTracking(chunk);
        }
    }

    /**
     * Sends this message to all clients tracking the given chunk.
     *
     * @param chunk the chunk that the target clients are tracking.
     */
    public void sendToAllTracking(@Nonnull Chunk chunk) {
        this.getNetwork().getNetworkChannel().send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), this);
    }

    /**
     * Sends this message to the all clients in the given dimension.
     *
     * @param dimension the dimension this message should be sent to.
     */
    public void sendToDimension(@Nonnull RegistryKey<World> dimension) {
        this.getNetwork().getNetworkChannel().send(PacketDistributor.DIMENSION.with(() -> dimension), this);
    }

    /**
     * Sends this message to the server to be executed.
     */
    public void sendToServer() {
        this.getNetwork().getNetworkChannel().sendToServer(this);
    }

}
