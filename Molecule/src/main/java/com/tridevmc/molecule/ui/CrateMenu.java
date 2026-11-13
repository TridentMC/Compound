package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.container.CompoundContainerMenu;
import com.tridevmc.molecule.block.CrateBlockEntity;
import com.tridevmc.molecule.init.MoleculeContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;


public class CrateMenu extends CompoundContainerMenu {

    private final CrateBlockEntity crate;
    private final Container inventory;

    public CrateMenu(int window, Inventory playerInv, BlockEntity tile) {
        super(MoleculeContent.CRATE_MENU, window);
        this.crate = tile instanceof CrateBlockEntity ? (CrateBlockEntity) tile : null;
        this.inventory = crate.inventory;

        // Actual inventory
        for (int k = 0; k < 3; ++k) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(this.inventory, l + k * 9, this.slots.size(), 0));
            }
        }

        // Player inventory
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int k1 = 0; k1 < 9; ++k1) {
                this.addSlot(new Slot(playerInv, k1 + i1 * 9 + 9, this.slots.size(), 0));
            }
        }

        // Hotbar
        for (int j1 = 0; j1 < 9; ++j1) {
            this.addSlot(new Slot(playerInv, j1, this.slots.size(), 0));
        }
    }

    public CrateMenu(int window, Inventory playerInventory, FriendlyByteBuf data) {
        this(window, playerInventory, playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.crate.getLevel().getBlockEntity(this.crate.getBlockPos()) != this.crate) {
            return false;
        } else {
            return player.distanceToSqr((double) this.crate.getBlockPos().getX() + 0.5D,
                    (double) this.crate.getBlockPos().getY() + 0.5D,
                    (double) this.crate.getBlockPos().getZ() + 0.5D) <= 64D;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack out = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stackSlot = slot.getItem();
            out = stackSlot.copy();
            if (index < this.inventory.getContainerSize()) {
                if (!this.moveItemStackTo(stackSlot, this.inventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackSlot, 0, this.inventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (stackSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return out;
    }

}
