package com.koenigstag.pull_cart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * MC+ Loom's wooden loom (TileEntityLoom, andykuo1, GPL-2.0): it spins a block of wool into 4 string in 20 seconds. It
 * works like a furnace whose fuel is two spindles at once (a stick, a blaze rod or shears in each slot): both are used
 * up together and turn it for as long as the two of them last.
 */
public class LoomBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUT = 0;
    public static final int SPINDLE_A = 1;
    public static final int SPINDLE_B = 2;
    public static final int RESULT = 3;
    /** Ticks to spin one block of wool. */
    public static final int SPIN_TIME = 400;
    /** Experience for each string taken out. */
    static final float EXPERIENCE = 0.5f;
    /** MC+ Loom turned the wheel 0.008 radians a frame; this is that at 60 frames a second. */
    private static final float WHEEL_SPEED = 0.024f;

    private static final int[] SLOTS_UP = {INPUT};
    private static final int[] SLOTS_DOWN = {RESULT};
    private static final int[] SLOTS_SIDES = {SPINDLE_B, SPINDLE_A};

    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int burnTime;
    private int burnDuration;
    private int spinTime;
    private float wheelAngle;
    private float wheelAngleO;
    private LazyOptional<? extends IItemHandlerModifiable>[] handlers =
            SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> spinTime;
                case 1 -> burnTime;
                case 2 -> burnDuration;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> spinTime = value;
                case 1 -> burnTime = value;
                case 2 -> burnDuration = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public LoomBlockEntity(BlockPos pos, BlockState state) {
        super(PullCartMod.LOOM_BLOCK_ENTITY.get(), pos, state);
    }

    /** How many ticks a spindle turns the loom; 0 if the item isn't one. */
    public static int spindleTime(ItemStack stack) {
        if (stack.is(Tags.Items.RODS_WOODEN)) {
            return 200;
        }
        if (stack.is(Tags.Items.RODS_BLAZE)) {
            return 800;
        }
        if (stack.is(Tags.Items.SHEARS)) {
            return 3200;
        }
        return 0;
    }

    public static boolean isSpindle(ItemStack stack) {
        return spindleTime(stack) > 0;
    }

    /** What the loom makes of the item, or empty: any wool, 4 string. */
    public static ItemStack spun(ItemStack input) {
        return input.is(ItemTags.WOOL) ? new ItemStack(Items.STRING, 4) : ItemStack.EMPTY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LoomBlockEntity loom) {
        boolean changed = false;
        if (loom.burnTime > 0) {
            loom.burnTime--;
        }
        if (loom.burnTime == 0 && loom.canSpin()) {
            int a = spindleTime(loom.items.get(SPINDLE_A));
            int b = spindleTime(loom.items.get(SPINDLE_B));
            if (a > 0 && b > 0) {
                loom.burnTime = loom.burnDuration = a + b;
                loom.useSpindle(SPINDLE_A);
                loom.useSpindle(SPINDLE_B);
                changed = true;
            }
        }
        boolean working = loom.burnTime > 0 && loom.canSpin();
        if (working) {
            if (++loom.spinTime >= SPIN_TIME) {
                loom.spinTime = 0;
                loom.spin();
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.05f, 0.6f);
                changed = true;
            }
        } else {
            loom.spinTime = 0;
        }
        if (state.getValue(LoomBlock.LIT) != working) {
            state = state.setValue(LoomBlock.LIT, working);
            level.setBlock(pos, state, 3);
            changed = true;
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    /** The wheel turns, and puffs of smoke rise, while it spins. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, LoomBlockEntity loom) {
        loom.wheelAngleO = loom.wheelAngle;
        if (!state.getValue(LoomBlock.LIT)) {
            return;
        }
        loom.wheelAngle += WHEEL_SPEED;
        if (loom.wheelAngle > Mth.TWO_PI) {
            loom.wheelAngle -= Mth.TWO_PI;
            loom.wheelAngleO -= Mth.TWO_PI;
        }
        if (level.random.nextInt(3) == 0) {
            float side = level.random.nextFloat() * 0.6f - 0.3f;
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 - side,
                    pos.getY() + 0.5 + level.random.nextFloat() * 6 / 16f, pos.getZ() + 0.5 - side / 2, 0, 0, 0);
        }
    }

    public float wheelAngle(float partialTick) {
        return Mth.lerp(partialTick, wheelAngleO, wheelAngle);
    }

    private boolean canSpin() {
        ItemStack result = spun(items.get(INPUT));
        if (result.isEmpty()) {
            return false;
        }
        ItemStack out = items.get(RESULT);
        return out.isEmpty() || ItemStack.isSameItemSameTags(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void spin() {
        ItemStack input = items.get(INPUT);
        ItemStack result = spun(input);
        ItemStack out = items.get(RESULT);
        if (out.isEmpty()) {
            items.set(RESULT, result);
        } else {
            out.grow(result.getCount());
        }
        input.shrink(1);
    }

    private void useSpindle(int slot) {
        ItemStack spindle = items.get(slot);
        ItemStack remainder = spindle.getCraftingRemainingItem();
        spindle.shrink(1);
        if (spindle.isEmpty()) {
            items.set(slot, remainder);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        spinTime = tag.getInt("SpinTime");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.putInt("SpinTime", spinTime);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pull_cart.wooden_loom");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new LoomMenu(id, inventory, this, data);
    }

    // -------------------------------------------------------------------------------------------------- container

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case RESULT -> false;
            case SPINDLE_A, SPINDLE_B -> isSpindle(stack);
            default -> true;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // Hoppers: wool in from the top, spindles from the sides, string out of the bottom.

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : side == Direction.UP ? SLOTS_UP : SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (!remove && side != null && cap == ForgeCapabilities.ITEM_HANDLER) {
            return switch (side) {
                case UP -> handlers[0].cast();
                case DOWN -> handlers[1].cast();
                default -> handlers[2].cast();
            };
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<? extends IItemHandlerModifiable> handler : handlers) {
            handler.invalidate();
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        handlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    }
}
