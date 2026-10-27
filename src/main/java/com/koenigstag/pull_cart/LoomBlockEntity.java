package com.koenigstag.pull_cart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * MC+ Loom's wooden loom (TileEntityLoom, andykuo1, GPL-2.0): it spins what goes on top by a {@link SpinningRecipe}
 * (a block of wool into 4 string in 20 seconds). It works like a furnace whose fuel is two spindles at once
 * ({@link SpindleRecipe}: a stick, a blaze rod or shears in each slot): both are used together and turn it for as long
 * as the two of them last.
 */
public class LoomBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUT = 0;
    public static final int SPINDLE_A = 1;
    public static final int SPINDLE_B = 2;
    public static final int RESULT = 3;
    /** MC+ Loom turned the wheel 0.008 radians a frame; this is that at 60 frames a second. */
    private static final float WHEEL_SPEED = 0.024f;

    private static final int[] SLOTS_UP = {INPUT};
    private static final int[] SLOTS_DOWN = {RESULT};
    private static final int[] SLOTS_SIDES = {SPINDLE_B, SPINDLE_A};

    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int burnTime;
    private int burnDuration;
    private int spinTime;
    private int spinTotalTime = SpinningRecipe.DEFAULT_SPINNING_TIME;
    /** Earned by what it spun, given to the next player who takes the result out. */
    private float experience;
    private float wheelAngle;
    private float wheelAngleO;

    final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> spinTime;
                case 1 -> burnTime;
                case 2 -> burnDuration;
                case 3 -> spinTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> spinTime = value;
                case 1 -> burnTime = value;
                case 2 -> burnDuration = value;
                case 3 -> spinTotalTime = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public LoomBlockEntity(BlockPos pos, BlockState state) {
        super(PullCartMod.LOOM_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LoomBlockEntity loom) {
        boolean changed = false;
        if (loom.burnTime > 0) {
            loom.burnTime--;
        }
        SpinningRecipe recipe = SpinningRecipe.find(level, loom.items.get(INPUT));
        boolean canSpin = recipe != null && loom.fits(recipe.result());
        if (loom.burnTime == 0 && canSpin) {
            SpindleRecipe a = SpindleRecipe.find(level, loom.items.get(SPINDLE_A));
            SpindleRecipe b = SpindleRecipe.find(level, loom.items.get(SPINDLE_B));
            if (a != null && b != null) {
                loom.burnTime = loom.burnDuration = a.duration() + b.duration();
                loom.useSpindle(SPINDLE_A, a, (ServerLevel) level);
                loom.useSpindle(SPINDLE_B, b, (ServerLevel) level);
                changed = true;
            }
        }
        boolean working = loom.burnTime > 0 && canSpin;
        if (working) {
            loom.spinTotalTime = recipe.spinningTime();
            if (++loom.spinTime >= loom.spinTotalTime) {
                loom.spinTime = 0;
                loom.spin(recipe);
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

    private boolean fits(ItemStack result) {
        ItemStack out = items.get(RESULT);
        return out.isEmpty() || ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void spin(SpinningRecipe recipe) {
        ItemStack out = items.get(RESULT);
        if (out.isEmpty()) {
            items.set(RESULT, recipe.result().copy());
        } else {
            out.grow(recipe.result().getCount());
        }
        items.get(INPUT).shrink(1);
        experience += recipe.experience();
    }

    private void useSpindle(int slot, SpindleRecipe spindle, ServerLevel level) {
        ItemStack stack = items.get(slot);
        if (spindle.damage() > 0 && stack.isDamageableItem()) {
            stack.hurtAndBreak(spindle.damage(), level, (ServerPlayer) null, item -> {
            });
            return;
        }
        ItemStack remainder = stack.getCraftingRemainingItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            items.set(slot, remainder);
        }
    }

    /** Gives out the experience earned so far, as orbs at {@code pos}. */
    public void popExperience(ServerLevel level, Vec3 pos) {
        int orbs = Mth.floor(experience);
        if (level.random.nextFloat() < Mth.frac(experience)) {
            orbs++;
        }
        experience = 0;
        ExperienceOrb.award(level, pos, orbs);
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        spinTime = tag.getInt("SpinTime");
        spinTotalTime = tag.contains("SpinTimeTotal") ? tag.getInt("SpinTimeTotal") : SpinningRecipe.DEFAULT_SPINNING_TIME;
        experience = tag.getFloat("Experience");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.putInt("SpinTime", spinTime);
        tag.putInt("SpinTimeTotal", spinTotalTime);
        tag.putFloat("Experience", experience);
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
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        boolean sameInput = slot == INPUT && ItemStack.isSameItemSameComponents(stack, items.get(INPUT));
        super.setItem(slot, stack);
        if (slot == INPUT && !sameInput) {
            spinTime = 0; // something else starts spinning from scratch, as in a furnace
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case RESULT -> false;
            case SPINDLE_A, SPINDLE_B -> level != null && SpindleRecipe.find(level, stack) != null;
            default -> true;
        };
    }

    // Hoppers: what to spin in from the top, spindles from the sides, the result out of the bottom.

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
}
