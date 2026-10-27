package com.koenigstag.pull_cart;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * A furnace riding on the cart, as in MC+ Cart: it smelts while you pull it. The vanilla furnace screen works on it
 * (input, fuel and result slots, burn and cook progress).
 */
final class CartFurnace implements Container {
    static final int INPUT = 0;
    static final int FUEL = 1;
    static final int RESULT = 2;

    private final CartEntity cart;
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int litTime;
    private int litDuration;
    private int cookingProgress;
    private int cookingTotalTime;

    final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookingProgress;
                case 3 -> cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookingProgress = value;
                case 3 -> cookingTotalTime = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    CartFurnace(CartEntity cart) {
        this.cart = cart;
    }

    boolean lit() {
        return litTime > 0;
    }

    void tick(Level level) {
        if (litTime > 0) {
            litTime--;
        }
        ItemStack input = items.get(INPUT);
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SmeltingRecipe>> recipe = input.isEmpty() ? Optional.empty()
                : level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
        ItemStack result = recipe.map(r -> r.value().assemble(recipeInput, level.registryAccess())).orElse(ItemStack.EMPTY);
        boolean canSmelt = !result.isEmpty() && fits(result);
        if (!lit() && canSmelt) {
            ItemStack fuel = items.get(FUEL);
            litTime = litDuration = fuel.getBurnTime(RecipeType.SMELTING);
            if (lit()) {
                if (fuel.hasCraftingRemainingItem() && fuel.getCount() == 1) {
                    items.set(FUEL, fuel.getCraftingRemainingItem());
                } else {
                    fuel.shrink(1);
                }
            }
        }
        if (lit() && canSmelt) {
            cookingTotalTime = recipe.get().value().getCookingTime();
            if (++cookingProgress >= cookingTotalTime) {
                cookingProgress = 0;
                ItemStack out = items.get(RESULT);
                if (out.isEmpty()) {
                    items.set(RESULT, result.copy());
                } else {
                    out.grow(result.getCount());
                }
                input.shrink(1);
            }
        } else if (!lit() && cookingProgress > 0) {
            cookingProgress = Mth.clamp(cookingProgress - 2, 0, cookingTotalTime);
        } else if (!canSmelt) {
            cookingProgress = 0;
        }
        cart.setLit(lit());
    }

    private boolean fits(ItemStack result) {
        ItemStack out = items.get(RESULT);
        return out.isEmpty() || ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    void save(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("BurnTime", litTime);
        tag.putInt("BurnDuration", litDuration);
        tag.putInt("CookTime", cookingProgress);
        tag.putInt("CookTimeTotal", cookingTotalTime);
    }

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        litTime = tag.getInt("BurnTime");
        litDuration = tag.getInt("BurnDuration");
        cookingProgress = tag.getInt("CookTime");
        cookingTotalTime = tag.getInt("CookTimeTotal");
    }

    /** Everything in it, emptied out (the cart drops it when the furnace comes off). */
    NonNullList<ItemStack> takeAll() {
        NonNullList<ItemStack> all = NonNullList.create();
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) {
                all.add(items.get(slot));
                items.set(slot, ItemStack.EMPTY);
            }
        }
        litTime = litDuration = cookingProgress = cookingTotalTime = 0;
        return all;
    }

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
        boolean sameInput = slot == INPUT && ItemStack.isSameItemSameComponents(stack, items.get(INPUT));
        items.set(slot, stack);
        if (slot == INPUT && !sameInput) {
            cookingProgress = 0; // a new item starts cooking from scratch, as in a furnace
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == RESULT) {
            return false;
        }
        if (slot == FUEL) {
            return stack.getBurnTime(RecipeType.SMELTING) > 0
                    || stack.is(Items.BUCKET) && !items.get(FUEL).is(Items.BUCKET);
        }
        return true;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return cart.isNear(player) && cart.cargo() == CargoType.FURNACE;
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
