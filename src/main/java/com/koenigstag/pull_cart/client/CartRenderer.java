package com.koenigstag.pull_cart.client;

import com.koenigstag.pull_cart.CartEntity;
import com.koenigstag.pull_cart.PullCartMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The cart as MC+ Cart drew it: the body model, a wooden wheel turning on each side, and the carried block on the bed
 * (the block's own look: a lit furnace burns, a chest is the chest you put on).
 */
public class CartRenderer extends EntityRenderer<CartEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PullCartMod.MOD_ID, "textures/entity/wooden_cart.png");
    /** Model units from the body's origin down to the bottom of the legs. */
    private static final float BODY_HEIGHT = 11 / 16f;
    /** The bed the block and a passenger sit on. */
    private static final float BED = 0.5f;
    private static final float CARGO_SCALE = 0.75f;
    private static final float WHEEL_BACK = 0.125f;
    private static final float WHEEL_SIDE = 0.7f;

    private final CartModel model;
    private final ItemRenderer itemRenderer;
    private ItemStack wheel = ItemStack.EMPTY;

    public CartRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CartModel(context.bakeLayer(CartModel.LAYER));
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.6f;
    }

    @Override
    public void render(CartEntity cart, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        // Facing: +X of the turned frame points at the one pulling (the shafts' side).
        float facing = -90f - yaw;
        float wheelRadius = (float) CartEntity.WHEEL_DIAMETER / 2;

        // After a hit it rocks about its length, on the axle, as a minecart or boat does.
        pose.pushPose();
        float wobble = cart.wobble(partialTick);
        if (wobble != 0) {
            pose.translate(0, wheelRadius, 0);
            pose.mulPose(Axis.YP.rotationDegrees(facing));
            pose.mulPose(Axis.XP.rotationDegrees(wobble));
            pose.mulPose(Axis.YP.rotationDegrees(-facing));
            pose.translate(0, -wheelRadius, 0);
        }

        pose.pushPose();
        pose.translate(0, BODY_HEIGHT, 0);
        pose.mulPose(Axis.YP.rotationDegrees(facing));
        pose.scale(-1, -1, 1);
        model.renderToBuffer(pose, buffers.getBuffer(model.renderType(TEXTURE)), light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.popPose();

        if (wheel.isEmpty()) {
            wheel = new ItemStack(PullCartMod.WHEEL.get());
        }
        for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            pose.translate(0, wheelRadius, 0);
            pose.mulPose(Axis.YP.rotationDegrees(facing));
            pose.translate(-WHEEL_BACK, 0, side * WHEEL_SIDE);
            pose.mulPose(Axis.ZP.rotationDegrees(-cart.wheelAngle(partialTick)));
            pose.scale((float) CartEntity.WHEEL_DIAMETER, (float) CartEntity.WHEEL_DIAMETER, (float) CartEntity.WHEEL_DIAMETER);
            itemRenderer.renderStatic(wheel, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                    cart.level(), cart.getId() * 2 + side);
            pose.popPose();
        }

        BlockState cargo = cart.cargoState();
        if (cargo != null) {
            pose.pushPose();
            pose.translate(0, BED, 0);
            pose.mulPose(Axis.YP.rotationDegrees(facing + 90f));
            pose.scale(CARGO_SCALE, CARGO_SCALE, CARGO_SCALE);
            pose.translate(-0.5, 0, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(cargo, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.popPose();
        super.render(cart, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CartEntity cart) {
        return TEXTURE;
    }
}
