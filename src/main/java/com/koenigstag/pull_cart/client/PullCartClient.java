package com.koenigstag.pull_cart.client;

import com.koenigstag.pull_cart.PullCartMod;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client bootstrap. Only ever loaded on the physical client. */
public final class PullCartClient {
    private PullCartClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(PullCartClient::onRegisterLayers);
        modBus.addListener(PullCartClient::onRegisterRenderers);
        modBus.addListener(PullCartClient::onRegisterScreens);
        modBus.addListener(PullCartClient::onRegisterClientExtensions);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CartModel.LAYER, CartModel::createLayer);
        event.registerLayerDefinition(LoomModel.LAYER, LoomModel::createLayer);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PullCartMod.CART.get(), CartRenderer::new);
        event.registerBlockEntityRenderer(PullCartMod.LOOM_BLOCK_ENTITY.get(), LoomRenderer::new);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(PullCartMod.LOOM_MENU.get(), LoomScreen::new);
    }

    /** The loom in your hand looks like the loom, as MC+ Loom drew it: through its block entity renderer. */
    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return LoomItemRenderer.get();
            }
        }, PullCartMod.LOOM_ITEM.get());
    }
}
