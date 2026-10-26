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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * {@code pull_cart:spindle}: what the loom takes as a spindle, and for how long it turns it.
 * <pre>
 * {"type": "pull_cart:spindle", "ingredient": {"item": "minecraft:stick"}, "duration": 200}
 * {"type": "pull_cart:spindle", "ingredient": {"item": "tfc:spindle"}, "duration": 400, "damage": 1}
 * </pre>
 * The loom takes one spindle from each of its two slots at a time and turns for both durations added up. A spindle is
 * used up, unless the recipe gives "damage": then a spindle that can be damaged takes that much damage instead.
 */
public record SpindleRecipe(ResourceLocation id, Ingredient ingredient, int duration, int damage) implements Recipe<Container> {
    /** The first recipe that makes this item a spindle, or null. */
    @Nullable
    public static SpindleRecipe find(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (SpindleRecipe recipe : level.getRecipeManager().getAllRecipesFor(PullCartMod.SPINDLE.get())) {
            if (recipe.ingredient.test(stack)) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return ingredient.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return ItemStack.EMPTY;
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
        return PullCartMod.SPINDLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return PullCartMod.SPINDLE.get();
    }

    public static final class Serializer implements RecipeSerializer<SpindleRecipe> {
        @Override
        public SpindleRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getNonNull(json, "ingredient"));
            int duration = GsonHelper.getAsInt(json, "duration");
            int damage = GsonHelper.getAsInt(json, "damage", 0);
            if (duration <= 0) {
                throw new JsonSyntaxException("duration must be positive, was " + duration);
            }
            if (damage < 0) {
                throw new JsonSyntaxException("damage can't be negative, was " + damage);
            }
            return new SpindleRecipe(id, ingredient, duration, damage);
        }

        @Override
        public SpindleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SpindleRecipe(id, Ingredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SpindleRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeVarInt(recipe.duration);
            buffer.writeVarInt(recipe.damage);
        }
    }
}
