package com.github.catbert.tlmv.inventory;

import com.github.catbert.tlmv.init.ModMenuTypes;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.core.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class MaidAltarMenu extends AbstractContainerMenu {

    private final Container container;
    private final BlockPos pos;

    public MaidAltarMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, getContainer(playerInventory.player.level(), pos), pos);
    }

    public MaidAltarMenu(int containerId, Inventory playerInventory, Container container) {
        this(containerId, playerInventory, container,
                container instanceof BlockEntity be ? be.getBlockPos() : null);
    }

    private MaidAltarMenu(int containerId, Inventory playerInventory, Container container, BlockPos pos) {
        super(ModMenuTypes.MAID_ALTAR.get(), containerId);
        this.container = container;
        this.pos = pos;
        checkContainerSize(container, 3);

        // 3个物品槽位
        this.addSlot(new Slot(container, 0, 44, 34) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModTags.Items.PURE_BLOOD);
            }
        });
        this.addSlot(new Slot(container, 1, 80, 34) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.HUMAN_HEART.get());
            }
        });
        this.addSlot(new Slot(container, 2, 116, 34) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.VAMPIRE_BOOK.get());
            }
        });

        // 玩家背包
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 玩家快捷栏
        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    private static Container getContainer(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container c ? c : new SimpleContainer(3);
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(itemstack1, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }
}
