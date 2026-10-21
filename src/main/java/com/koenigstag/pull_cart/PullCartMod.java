package com.koenigstag.pull_cart;

import com.koenigstag.pull_cart.client.PullCartClient;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * A wooden cart you pull by hand. A Minecraft 1.20.1 port of MC+ Cart and MC+ Wheel by andykuo1 (GPL-2.0,
 * github.com/andykuo1/mcplus-mods), the cart TFC.SU played with on 1.7.10.
 */
@Mod(PullCartMod.MOD_ID)
public final class PullCartMod {
    public static final String MOD_ID = "pull_cart";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean TFC = ModList.get().isLoaded("tfc");

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<EntityType<CartEntity>> CART = ENTITY_TYPES.register("wooden_cart",
            () -> EntityType.Builder.<CartEntity>of(CartEntity::new, MobCategory.MISC)
                    // Narrower than the model, so it follows you through one-block passages.
                    .sized(0.98f, 0.8f)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(MOD_ID + ":wooden_cart"));
    public static final RegistryObject<Item> CART_ITEM = ITEMS.register("wooden_cart",
            () -> new CartItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WHEEL = ITEMS.register("wooden_wheel",
            () -> new Item(new Item.Properties()));

    public PullCartMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(PullCartMod::onCreativeTabs);
        context.registerConfig(ModConfig.Type.COMMON, PullCartConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PullCartClient.init(modBus);
        }
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CART_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(WHEEL);
        }
    }
}
