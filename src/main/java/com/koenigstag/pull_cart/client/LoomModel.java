package com.koenigstag.pull_cart.client;

import com.koenigstag.pull_cart.PullCartMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The loom: a base, a back panel, two arms and the wheel between them, the geometry and texture layout of MC+ Loom's
 * ModelLoom and ModelLoomWheel (andykuo1, GPL-2.0). The wheel turns about X.
 */
public class LoomModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PullCartMod.MOD_ID, "wooden_loom"), "main");

    private final ModelPart root;
    private final ModelPart wheel;

    public LoomModel(ModelPart root) {
        this.root = root;
        this.wheel = root.getChild("wheel");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-7, 0, -8, 14, 6, 16),
                PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("back_panel", CubeListBuilder.create().texOffs(0, 22).addBox(-8, -7, 0, 16, 14, 2),
                PartPose.offset(0, 17, 6));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(34, 48).addBox(0, -2, 0, 3, 4, 12),
                PartPose.offset(2, 16, -5));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(34, 48).addBox(-3, -2, 0, 3, 4, 12),
                PartPose.offset(-2, 16, -5));
        root.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(0, 40).addBox(-0.5f, -6, -6, 1, 12, 12),
                PartPose.offset(0, 13, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, float wheelAngle) {
        wheel.xRot = wheelAngle;
        root.render(pose, buffer, light, overlay);
    }
}
