package com.koenigstag.pull_cart.client;

import com.koenigstag.pull_cart.CartEntity;
import com.koenigstag.pull_cart.PullCartMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The cart body: an open box with two shafts at the front and two legs under them, the geometry and texture layout of
 * MC+ Cart's ModelCart (andykuo1, GPL-2.0). The shafts point along -X; the wheels are drawn separately.
 */
public class CartModel extends EntityModel<CartEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PullCartMod.MOD_ID, "wooden_cart"), "main");

    private final ModelPart root;

    public CartModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder side = CubeListBuilder.create().texOffs(0, 0).addBox(-10, -7, -1, 20, 6, 2);
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 8).addBox(-12, -8, -3, 24, 16, 4),
                PartPose.offsetAndRotation(0, 4, 0, Mth.HALF_PI, 0, 0));
        root.addOrReplaceChild("left", side, PartPose.offsetAndRotation(-11, 4, 0, 0, Mth.PI * 3 / 2, 0));
        root.addOrReplaceChild("right", side, PartPose.offsetAndRotation(11, 4, 0, 0, Mth.HALF_PI, 0));
        root.addOrReplaceChild("back", side, PartPose.offsetAndRotation(0, 4, -9, 0, Mth.PI, 0));
        root.addOrReplaceChild("front", side, PartPose.offsetAndRotation(0, 4, 9, 0, 0, 0));
        CubeListBuilder shaft = CubeListBuilder.create().texOffs(0, 0).addBox(0, -3, -6, 1, 3, 12);
        root.addOrReplaceChild("shaft_left", shaft, PartPose.offsetAndRotation(-18, 0, 9, 0, Mth.HALF_PI, 0));
        root.addOrReplaceChild("shaft_right", shaft, PartPose.offsetAndRotation(-18, 0, -8, 0, Mth.HALF_PI, 0));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 0).addBox(0, -3, -6, 1, 4, 3);
        root.addOrReplaceChild("leg_left", leg, PartPose.offsetAndRotation(-6, 10, 6, 0, Mth.HALF_PI, 0));
        root.addOrReplaceChild("leg_right", leg, PartPose.offsetAndRotation(-6, 10, -6, 0, Mth.HALF_PI, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(CartEntity cart, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(pose, buffer, light, overlay, color);
    }
}
