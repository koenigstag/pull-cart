package com.koenigstag.pull_cart.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The loom item: the loom's model facing south, its wheel still, as MC+ Loom's item renderer had it. */
public final class LoomItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static LoomItemRenderer instance;

    private LoomModel model;

    private LoomItemRenderer(Minecraft minecraft) {
        super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
    }

    public static LoomItemRenderer get() {
        if (instance == null) {
            instance = new LoomItemRenderer(Minecraft.getInstance());
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        if (model == null) {
            model = new LoomModel(Minecraft.getInstance().getEntityModels().bakeLayer(LoomModel.LAYER));
        }
        LoomRenderer.render(model, pose, buffers, light, overlay, 0, 0);
    }
}
