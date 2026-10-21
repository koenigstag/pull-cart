package com.koenigstag.pull_cart;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.JukeboxBlock;

/**
 * What a cart can carry, as in MC+ Cart: put the block on it with a right-click. Any chest or crafting table counts,
 * TFC's wooden ones included.
 */
public enum CargoType {
    NONE, CHEST, WORKBENCH, JUKEBOX, ENDER_CHEST, FURNACE;

    public static CargoType of(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return NONE;
        }
        Block block = item.getBlock();
        if (block instanceof EnderChestBlock) {
            return ENDER_CHEST;
        }
        if (block instanceof ChestBlock) {
            return CHEST;
        }
        if (block instanceof CraftingTableBlock) {
            return WORKBENCH;
        }
        if (block instanceof FurnaceBlock) {
            return FURNACE;
        }
        if (block instanceof JukeboxBlock) {
            return JUKEBOX;
        }
        return NONE;
    }
}
