package com.koenigstag.pull_cart.renderer;

import com.koenigstag.pull_cart.PullCartMod;
import com.koenigstag.pull_cart.entities.ModEntities;
import com.koenigstag.pull_cart.models.PullCartModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PullCartMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderers {
    public static final ModelLayerLocation PULL_CART_LAYER = new ModelLayerLocation(
            new ResourceLocation(PullCartMod.MODID, "pull_cart"), "main");

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PULL_CART_LAYER, PullCartModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PULL_CART.get(), PullCartRenderer::new);
    }
}