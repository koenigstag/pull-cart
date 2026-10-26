package com.koenigstag.pull_cart;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * {@code pull_cart:spinning}: what the loom spins, one item at a time.
 * <pre>
 * {"type": "pull_cart:spinning", "ingredient": {"tag": "minecraft:wool"}, "result": {"item": "minecraft:string", "count": 4},
 *  "experience": 2.0, "spinning_time": 400}
 * </pre>
 * Experience is for each item spun, and is given when you take the result out; spinning_time is in ticks (400 if left
 * out).
 */
public record SpinningRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result, float experience,
                             int spinningTime) implements Recipe<Container> {
    public static final int DEFAULT_SPINNING_TIME = 400;

    /** The first recipe that spins this item, or null. */
    @Nullable
    public static SpinningRecipe find(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        for (SpinningRecipe recipe : level.getRecipeManager().getAllRecipesFor(PullCartMod.SPINNING.get())) {
            if (recipe.ingredient.test(input)) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return ingredient.test(container.getItem(LoomBlockEntity.INPUT));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** Not in the recipe book. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return PullCartMod.SPINNING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return PullCartMod.SPINNING.get();
    }

    public static final class Serializer implements RecipeSerializer<SpinningRecipe> {
        @Override
        public SpinningRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getNonNull(json, "ingredient"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            float experience = GsonHelper.getAsFloat(json, "experience", 0);
            int time = GsonHelper.getAsInt(json, "spinning_time", DEFAULT_SPINNING_TIME);
            if (time <= 0) {
                throw new JsonSyntaxException("spinning_time must be positive, was " + time);
            }
            return new SpinningRecipe(id, ingredient, result, experience, time);
        }

        @Override
        public SpinningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SpinningRecipe(id, Ingredient.fromNetwork(buffer), buffer.readItem(), buffer.readFloat(),
                    buffer.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SpinningRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeFloat(recipe.experience);
            buffer.writeVarInt(recipe.spinningTime);
        }
    }
}
