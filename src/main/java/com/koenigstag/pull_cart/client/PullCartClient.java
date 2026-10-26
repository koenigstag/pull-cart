package com.koenigstag.pull_cart.client;

import com.koenigstag.pull_cart.PullCartMod;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client bootstrap. Only ever loaded on the physical client. */
public final class PullCartClient {
    private PullCartClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(PullCartClient::onRegisterLayers);
        modBus.addListener(PullCartClient::onRegisterRenderers);
        modBus.addListener(PullCartClient::onClientSetup);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CartModel.LAYER, CartModel::createLayer);
        event.registerLayerDefinition(LoomModel.LAYER, LoomModel::createLayer);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PullCartMod.CART.get(), CartRenderer::new);
        event.registerBlockEntityRenderer(PullCartMod.LOOM_BLOCK_ENTITY.get(), LoomRenderer::new);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(PullCartMod.LOOM_MENU.get(), LoomScreen::new));
    }
}
