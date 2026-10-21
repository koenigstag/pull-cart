package com.koenigstag.pull_cart;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/** The crafting table on a cart: the vanilla crafting screen, valid while the cart is near and still carries it. */
final class CartCraftingMenu extends CraftingMenu {
    private final CartEntity cart;

    CartCraftingMenu(int id, Inventory inventory, CartEntity cart) {
        super(id, inventory, ContainerLevelAccess.create(cart.level(), cart.blockPosition()));
        this.cart = cart;
    }

    @Override
    public boolean stillValid(Player player) {
        return cart.isNear(player) && cart.cargo() == CargoType.WORKBENCH;
    }
}
