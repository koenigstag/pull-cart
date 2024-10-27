package com.koenigstag.pull_cart.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class PullCartModel<T extends Entity> extends EntityModel<T> {

  private final ModelPart cart;
  private final ModelPart leftWheel;
  private final ModelPart rightWheel;

  public PullCartModel(ModelPart root) {
    this.cart = root.getChild("cart");
    this.leftWheel = root.getChild("left_wheel");
    this.rightWheel = root.getChild("right_wheel");
  }

  public static LayerDefinition createBodyLayer() {
    MeshDefinition meshdefinition = new MeshDefinition();
    PartDefinition partdefinition = meshdefinition.getRoot();

    // Создаем основное тело повозки на основе лодки
    partdefinition.addOrReplaceChild("cart",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-7.0F, -2.0F, -14.0F, 14.0F, 4.0F, 28.0F), // Размеры и положение
        PartPose.ZERO);

    // Добавляем колеса к модели
    partdefinition.addOrReplaceChild("left_wheel",
        CubeListBuilder.create()
            .texOffs(0, 32)
            .addBox(-8.0F, -1.0F, -13.0F, 2.0F, 2.0F, 2.0F), // Левое колесо
        PartPose.ZERO);

    partdefinition.addOrReplaceChild("right_wheel",
        CubeListBuilder.create()
            .texOffs(0, 32)
            .addBox(6.0F, -1.0F, -13.0F, 2.0F, 2.0F, 2.0F), // Правое колесо
        PartPose.ZERO);

    return LayerDefinition.create(meshdefinition, 64, 64);
  }

  @Override
  public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
      float headPitch) {
    // Устанавливаем вращение колёс в зависимости от скорости
    float rotation = ageInTicks * 10; // Скорость вращения колёс, можно настроить
    leftWheel.yRot = rotation;
    rightWheel.yRot = rotation;
  }

  @Override
  public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
      float red, float green, float blue, float alpha) {
    cart.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    leftWheel.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    rightWheel.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
  }
}
