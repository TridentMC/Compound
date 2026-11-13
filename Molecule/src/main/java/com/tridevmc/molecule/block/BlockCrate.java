package com.tridevmc.molecule.block;

import com.tridevmc.molecule.ui.CrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class BlockCrate extends BaseEntityBlock {

    public BlockCrate(Properties builder) {
        super(builder);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState pState, Level level, BlockPos pos, Player player, BlockHitResult pHitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CrateBlockEntity) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public @NotNull AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
                    return new CrateMenu(id, playerInv, level.getBlockEntity(pos));
                }

                @Override
                public @NotNull Component getDisplayName() {
                    return Component.empty();
                }
            }, pos);
        }

        return InteractionResult.SUCCESS;
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

}
