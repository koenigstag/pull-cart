package com.koenigstag.pull_cart.entities;

import com.koenigstag.pull_cart.PullCartMod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = PullCartMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    // Регистрация через Deferred Register
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
            PullCartMod.MODID);

    public static final RegistryObject<EntityType<PullCart>> PULL_CART = ENTITIES.register("pull_cart",
            () -> EntityType.Builder.<PullCart>of(PullCart::new, MobCategory.MISC)
                    .sized(1.375f, 1.2f) // Размеры лодки (ширина, высота)
                    .build("pull_cart"));

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);

        PullCartMod.LOGGER.info("Entities registered");
    }
}
