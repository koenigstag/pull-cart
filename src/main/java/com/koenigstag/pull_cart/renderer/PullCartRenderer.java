package com.koenigstag.pull_cart.renderer;

import com.koenigstag.pull_cart.PullCartMod;
import com.koenigstag.pull_cart.entities.PullCart;
import com.koenigstag.pull_cart.models.PullCartModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class PullCartRenderer extends EntityRenderer<PullCart> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(PullCartMod.MODID,
      "textures/entity/pull_cart.png");
  private final PullCartModel<PullCart> model;

  public PullCartRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.model = new PullCartModel<>(context.bakeLayer(ModEntityRenderers.PULL_CART_LAYER));
  }

  @Override
  public ResourceLocation getTextureLocation(PullCart entity) {
    return TEXTURE;
  }

  @Override
  public void render(PullCart entity, float entityYaw, float partialTicks, PoseStack poseStack,
      MultiBufferSource bufferSource, int packedLight) {
    poseStack.pushPose();
    poseStack.translate(0.0D, 1.0D, 0.0D);
    this.model.renderToBuffer(poseStack, bufferSource.getBuffer(this.model.renderType(getTextureLocation(entity))),
        packedLight, packedLight, 1.0F, 1.0F, 1.0F, 1.0F);
    poseStack.popPose();
    super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
  }
}
