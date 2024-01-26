/*
 * Copyright 2018 - 2022 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.network.message;

import com.tridevmc.compound.network.core.CompoundNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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

    public abstract void handle(@Nullable Player player);

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
    public void sendToMatching(@Nonnull Predicate<ServerPlayer> playerPredicate) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        server.getPlayerList().getPlayers().stream()
                .filter(playerPredicate)
                .forEach(this::sendTo);
    }

    private MessageConcept getMessageConcept() {
        return this.getNetwork().getMsgConcept(this);
    }

    /**
     * Sends this message to the client of the given player.
     *
     * @param player the player to send the message to.
     */
    public void sendTo(@Nonnull ServerPlayer player) {
        player.connection.send(this.getMessageConcept().createPayload(this));
    }

    /**
     * Sends this message to all clients around the given point in the given dimension.
     *
     * @param dimension the dimension the point is in.
     * @param pos       the coordinates of the point.
     * @param range     the range around the point that the target clients are within.
     */
    public void sendToAllAround(@Nonnull ResourceKey<Level> dimension, @Nonnull BlockPos pos, double range) {
        this.sendToMatching(p -> {
            ResourceKey<Level> playerDim = p.level().dimension();
            if (playerDim.equals(dimension)) {
                double dist = p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                return dist <= range * range;
            } else {
                return false;
            }
        });
    }

    /**
     * Sends this message to all clients tracking the given entity.
     *
     * @param entity the entity that the target clients are tracking.
     */
    public void sendToAllTracking(@Nonnull Entity entity) {
        this.sendToMatching(p -> p.level().dimension() == entity.level().dimension() && p.getChunkTrackingView().contains(entity.chunkPosition()));
    }

    /**
     * Sends this message to all clients tracking the given tile entity.
     *
     * @param tile the tile entity that the target clients are tracking.
     */
    public void sendToAllTracking(@Nonnull BlockEntity tile) {
        this.sendToAllTracking(tile.getLevel().getChunkAt(tile.getBlockPos()));
    }

    /**
     * Sends this message to all clients tracking the given position in the given dimension.
     *
     * @param dimension the dimension the point is in.
     * @param pos       the coordinates of the point.
     */
    public void sendToAllTracking(@Nonnull ResourceKey<Level> dimension, @Nonnull BlockPos pos) {
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        // Don't force load the level because if we have to load the level then nobody is tracking this to begin with.
        ServerLevel level = currentServer.getLevel(dimension);
        if (level != null) {
            LevelChunk chunk = level.getChunkAt(pos);
            this.sendToAllTracking(chunk);
        }
    }

    /**
     * Sends this message to all clients tracking the given chunk.
     *
     * @param chunk the chunk that the target clients are tracking.
     */
    public void sendToAllTracking(@Nonnull LevelChunk chunk) {
        this.sendToMatching(p -> p.level().dimension() == chunk.getLevel().dimension() && p.getChunkTrackingView().contains(chunk.getPos()));
    }

    /**
     * Sends this message to the all clients in the given dimension.
     *
     * @param dimension the dimension this message should be sent to.
     */
    public void sendToDimension(@Nonnull ResourceKey<Level> dimension) {
        this.sendToMatching(p -> p.level().dimension() == dimension);
    }

    /**
     * Sends this message to the server to be executed.
     */
    public void sendToServer() {
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(this.getMessageConcept().createPayload(this));
    }

}
