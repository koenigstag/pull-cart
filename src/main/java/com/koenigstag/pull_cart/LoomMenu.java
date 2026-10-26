package com.koenigstag.pull_cart;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The loom's screen, laid out as MC+ Loom's ContainerLoom: wool on top, the two spindles under it, string on the
 * right. Taking string out gives experience, as taking from a furnace does.
 */
public class LoomMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = 4;
    private static final int HOTBAR_START = 31;
    private static final int END = 40;

    private final Container loom;
    private final ContainerData data;

    /** The client side, filled in by the server. */
    public LoomMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(4), new SimpleContainerData(3));
    }

    LoomMenu(int id, Inventory inventory, Container loom, ContainerData data) {
        super(PullCartMod.LOOM_MENU.get(), id);
        checkContainerSize(loom, 4);
        checkContainerDataCount(data, 3);
        this.loom = loom;
        this.data = data;
        addSlot(new Slot(loom, LoomBlockEntity.INPUT, 56, 17));
        addSlot(new SpindleSlot(loom, LoomBlockEntity.SPINDLE_A, 47, 53));
        addSlot(new SpindleSlot(loom, LoomBlockEntity.SPINDLE_B, 65, 53));
        addSlot(new ResultSlot(inventory.player, loom, LoomBlockEntity.RESULT, 116, 35));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    public boolean isBurning() {
        return data.get(1) > 0;
    }

    /** 0 to {@code width}: how far the wool on top has been spun. */
    public int spinProgress(int width) {
        return data.get(0) * width / LoomBlockEntity.SPIN_TIME;
    }

    /** 0 to {@code height}: how much of the spindles' time is left. */
    public int burnLeft(int height) {
        int duration = data.get(2);
        return data.get(1) * height / (duration == 0 ? LoomBlockEntity.SPIN_TIME : duration);
    }

    @Override
    public boolean stillValid(Player player) {
        return loom.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == LoomBlockEntity.RESULT) {
            if (!moveItemStackTo(stack, PLAYER_START, END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index >= PLAYER_START) {
            if (!LoomBlockEntity.spun(stack).isEmpty()) {
                if (!moveItemStackTo(stack, LoomBlockEntity.INPUT, LoomBlockEntity.INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (LoomBlockEntity.isSpindle(stack)) {
                if (!moveItemStackTo(stack, LoomBlockEntity.SPINDLE_A, LoomBlockEntity.SPINDLE_B + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private static final class SpindleSlot extends Slot {
        SpindleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return LoomBlockEntity.isSpindle(stack);
        }
    }

    /** MC+ Loom's SlotLoom: nothing goes in, and what comes out gives experience. */
    private static final class ResultSlot extends Slot {
        private final Player player;
        private int removeCount;

        ResultSlot(Player player, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.player = player;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            if (hasItem()) {
                removeCount += Math.min(amount, getItem().getCount());
            }
            return super.remove(amount);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            checkTakeAchievements(stack);
            super.onTake(player, stack);
        }

        @Override
        protected void onQuickCraft(ItemStack stack, int amount) {
            removeCount += amount;
            checkTakeAchievements(stack);
        }

        @Override
        protected void checkTakeAchievements(ItemStack stack) {
            stack.onCraftedBy(player.level(), player, removeCount);
            if (player instanceof ServerPlayer server) {
                float experience = removeCount * LoomBlockEntity.EXPERIENCE;
                int orbs = Mth.floor(experience);
                if (Math.random() < experience - orbs) {
                    orbs++;
                }
                ExperienceOrb.award(server.serverLevel(), server.position(), orbs);
            }
            removeCount = 0;
        }
    }
}
