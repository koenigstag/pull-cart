package com.koenigstag.pull_cart;

import com.koenigstag.pull_cart.client.LoomItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** The loom in your hand looks like the loom, as MC+ Loom drew it: through its block entity renderer. */
public class LoomItem extends BlockItem {
    public LoomItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return LoomItemRenderer.get();
            }
        });
    }
}
