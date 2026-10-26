package com.koenigstag.pull_cart.client;

import com.koenigstag.pull_cart.LoomBlock;
import com.koenigstag.pull_cart.LoomBlockEntity;
import com.koenigstag.pull_cart.PullCartMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** The loom as MC+ Loom's RenderLoom drew it: the model turned to face its way, the wheel turning while it spins. */
public class LoomRenderer implements BlockEntityRenderer<LoomBlockEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PullCartMod.MOD_ID, "textures/entity/wooden_loom.png");

    private final LoomModel model;

    public LoomRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new LoomModel(context.bakeLayer(LoomModel.LAYER));
    }

    @Override
    public void render(LoomBlockEntity loom, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = loom.getBlockState();
        float facing = state.hasProperty(LoomBlock.FACING) ? state.getValue(LoomBlock.FACING).toYRot() : 0;
        render(model, pose, buffers, light, overlay, facing, loom.wheelAngle(partialTick));
    }

    /** Draws the loom in the block at the origin; facing south at 0 degrees. */
    static void render(LoomModel model, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float facing,
                       float wheelAngle) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(1, -1, -1);
        pose.mulPose(Axis.YP.rotationDegrees(facing));
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, overlay, wheelAngle);
        pose.popPose();
    }
}
