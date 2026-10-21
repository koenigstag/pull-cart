package com.koenigstag.pull_cart;

import net.minecraftforge.common.ForgeConfigSpec;

/** config/pull_cart-common.toml */
public final class PullCartConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue FOLLOW_GAP;
    public static final ForgeConfigSpec.DoubleValue LET_GO_DISTANCE;
    public static final ForgeConfigSpec.IntValue MAX_HEAVY_ITEMS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("pulling");
        FOLLOW_GAP = b.comment("Blocks between you and the cart while you pull it.")
                .defineInRange("followGap", 1.2, 0.5, 4.0);
        LET_GO_DISTANCE = b.comment("You let go of a cart that falls this far behind (it got stuck).")
                .defineInRange("letGoDistance", 6.0, 3.0, 32.0);
        b.pop();
        b.push("tfc");
        MAX_HEAVY_ITEMS = b.comment("With TerraFirmaCraft: the most huge, very heavy items (anvils and the like) a chest on",
                        "the cart may hold and still be pulled.")
                .defineInRange("maxHeavyItems", 4, 0, 1000);
        b.pop();
        SPEC = b.build();
    }

    private PullCartConfig() {
    }
}
