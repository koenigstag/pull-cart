package com.koenigstag.pull_cart;

import com.koenigstag.pull_cart.client.PullCartClient;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * A wooden cart you pull by hand, and a wooden loom that spins wool into string. A Minecraft 1.21.1 (NeoForge) port of
 * MC+ Cart, MC+ Wheel and MC+ Loom by andykuo1 (GPL-2.0, github.com/andykuo1/mcplus-mods), the cart TFC.SU played
 * with on 1.7.10.
 */
@Mod(PullCartMod.MOD_ID)
public final class PullCartMod {
    public static final String MOD_ID = "pull_cart";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean TFC = ModList.get().isLoaded("tfc");

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CartEntity>> CART = ENTITY_TYPES.register("wooden_cart",
            () -> EntityType.Builder.<CartEntity>of(CartEntity::new, MobCategory.MISC)
                    // Narrower than the model, so it follows you through one-block passages.
                    .sized(0.98f, 0.8f)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(MOD_ID + ":wooden_cart"));
    public static final DeferredHolder<Item, Item> CART_ITEM = ITEMS.register("wooden_cart",
            () -> new CartItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> WHEEL = ITEMS.register("wooden_wheel",
            () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Block, Block> LOOM = BLOCKS.register("wooden_loom",
            () -> new LoomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoomBlockEntity>> LOOM_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("wooden_loom",
                    () -> BlockEntityType.Builder.of(LoomBlockEntity::new, LOOM.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<LoomMenu>> LOOM_MENU = MENU_TYPES.register("wooden_loom",
            () -> IMenuTypeExtension.create((id, inventory, buffer) -> new LoomMenu(id, inventory)));
    public static final DeferredHolder<Item, Item> LOOM_ITEM = ITEMS.register("wooden_loom",
            () -> new BlockItem(LOOM.get(), new Item.Properties()));
    public static final DeferredHolder<RecipeType<?>, RecipeType<SpinningRecipe>> SPINNING = RECIPE_TYPES.register("spinning",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MOD_ID, "spinning")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpinningRecipe>> SPINNING_SERIALIZER =
            RECIPE_SERIALIZERS.register("spinning", SpinningRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<SpindleRecipe>> SPINDLE = RECIPE_TYPES.register("spindle",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MOD_ID, "spindle")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpindleRecipe>> SPINDLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("spindle", SpindleRecipe.Serializer::new);

    public PullCartMod(IEventBus modBus, ModContainer container) {
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
        ITEMS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener(PullCartMod::onCreativeTabs);
        modBus.addListener(PullCartMod::onRegisterCapabilities);
        container.registerConfig(ModConfig.Type.COMMON, PullCartConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PullCartClient.init(modBus);
        }
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CART_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(WHEEL.get());
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(LOOM_ITEM.get());
        }
    }

    /** Hoppers and pipes: the loom's sides, as a furnace's. */
    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LOOM_BLOCK_ENTITY.get(),
                (loom, side) -> side == null ? new InvWrapper(loom) : new SidedInvWrapper(loom, side));
    }
}
