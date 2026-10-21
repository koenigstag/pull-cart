package com.koenigstag.pull_cart.compat;

import net.dries007.tfc.util.Helpers;
import net.minecraft.world.Container;

/** TerraFirmaCraft's weight rules. Only call it when TFC is loaded. */
public final class TfcWeight {
    private TfcWeight() {
    }

    /** Items TFC calls overburdening (huge and very heavy: anvils and the like) in the container. */
    public static int overburdening(Container container) {
        return Helpers.countOverburdened(container);
    }
}
