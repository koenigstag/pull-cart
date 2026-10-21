package com.koenigstag.pull_cart.client;

import com.koenigstag.pull_cart.PullCartMod;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client bootstrap. Only ever loaded on the physical client. */
public final class PullCartClient {
    private PullCartClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(PullCartClient::onRegisterLayers);
        modBus.addListener(PullCartClient::onRegisterRenderers);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CartModel.LAYER, CartModel::createLayer);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PullCartMod.CART.get(), CartRenderer::new);
    }
}
