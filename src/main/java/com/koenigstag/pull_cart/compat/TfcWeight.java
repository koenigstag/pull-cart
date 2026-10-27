package com.koenigstag.pull_cart.compat;

import net.dries007.tfc.common.component.size.IItemSize;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** TerraFirmaCraft's weight rules. Only call it when TFC is loaded. */
public final class TfcWeight {
    private TfcWeight() {
    }

    /** Items TFC calls overburdening (huge and very heavy: anvils and the like) in the container. */
    public static int overburdening(Container container) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                IItemSize size = ItemSizeManager.get(stack);
                if (size.getWeight(stack) == Weight.VERY_HEAVY && size.getSize(stack) == Size.HUGE) {
                    count++;
                }
            }
        }
        return count;
    }
}
