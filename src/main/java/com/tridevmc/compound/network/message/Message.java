package com.tridevmc.compound.network.message;

import com.tridevmc.compound.network.core.CompoundNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
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

    public abstract void handle(@Nullable EntityPlayer player);

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
    public void sendToMatching(@Nonnull Predicate<EntityPlayerMP> playerPredicate) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
                .filter(playerPredicate)
                .forEach(this::sendTo);
    }

    /**
     * Sends this message to the client of the given player.
     *
     * @param player the player to send the message to.
     */
    public void sendTo(@Nonnull EntityPlayerMP player) {
        this.getNetwork().getNetworkChannel().sendTo(this, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    /**
     * Sends this message to all clients around the given point in the given dimension.
     *
     * @param dimension the dimension the point is in.
     * @param pos       the coordinates of the point.
     * @param range     the range around the point that the target clients are within.
     */
    public void sendToAllAround(@Nonnull DimensionType dimension, @Nonnull BlockPos pos, double range) {
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        WorldServer world = DimensionManager.getWorld(currentServer, dimension, false, false);
        if (world != null) {
            BlockPos min = pos.subtract(new Vec3i(range, 0, range));
            BlockPos max = pos.add(new Vec3i(range, 0, range));

            currentServer.getPlayerList().getPlayers().stream().filter((e) -> {
                BlockPos ePos = e.getPosition();
                return ePos.getX() >= min.getX() && ePos.getX() <= max.getX() &&
                        ePos.getZ() >= min.getZ() && ePos.getZ() <= max.getZ();
            }).forEach(this::sendTo);
        }
    }

    /**
     * Sends this message to all clients tracking the given entity.
     *
     * @param entity the entity that the target clients are tracking.
     */
    public void sendToAllTracking(@Nonnull Entity entity) {
        this.sendToAllTracking(entity.dimension, entity.getPosition());
    }

    /**
     * Sends this message to all clients tracking the given tile entity.
     *
     * @param tile the tile entity that the target clients are tracking.
     */
    public void sendToAllTracking(@Nonnull TileEntity tile) {
        this.sendToAllTracking(tile.getWorld().getDimension().getType(), tile.getPos());
    }

    /**
     * Sends this message to all clients tracking the given position in the given dimension.
     *
     * @param dimension the dimension the point is in.
     * @param pos       the coordinates of the point.
     */
    public void sendToAllTracking(@Nonnull DimensionType dimension, @Nonnull BlockPos pos) {
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        WorldServer world = DimensionManager.getWorld(currentServer, dimension, false, false);
        if (world != null) {
            ChunkPos chunkPos = new ChunkPos(pos);
            currentServer.getPlayerList().getPlayers().stream()
                    .filter((e) -> world.getPlayerChunkMap().isPlayerWatchingChunk(e, chunkPos.x, chunkPos.z))
                    .forEach(this::sendTo);
        }
    }

    /**
     * Sends this message to the all clients in the given dimension.
     *
     * @param dimension the dimension this message should be sent to.
     */
    public void sendToDimension(@Nonnull DimensionType dimension) {
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        currentServer.getPlayerList().getPlayers().stream()
                .filter((e) -> Objects.equals(e.dimension, dimension))
                .forEach(this::sendTo);
    }

    /**
     * Sends this message to the server to be executed.
     */
    public void sendToServer() {
        getNetwork().getNetworkChannel().sendToServer(this);
    }

}
