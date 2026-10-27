package com.koenigstag.pull_cart;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
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
public record SpindleRecipe(Ingredient ingredient, int duration, int damage) implements Recipe<SingleRecipeInput> {
    /** The recipe that makes this item a spindle, or null. */
    @Nullable
    public static SpindleRecipe find(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return level.getRecipeManager().getRecipeFor(PullCartMod.SPINDLE.get(), new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value).orElse(null);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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
    public RecipeSerializer<?> getSerializer() {
        return PullCartMod.SPINDLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return PullCartMod.SPINDLE.get();
    }

    public static final class Serializer implements RecipeSerializer<SpindleRecipe> {
        private static final MapCodec<SpindleRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SpindleRecipe::ingredient),
                ExtraCodecs.POSITIVE_INT.fieldOf("duration").forGetter(SpindleRecipe::duration),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("damage", 0).forGetter(SpindleRecipe::damage)
        ).apply(instance, SpindleRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, SpindleRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SpindleRecipe::ingredient,
                ByteBufCodecs.VAR_INT, SpindleRecipe::duration,
                ByteBufCodecs.VAR_INT, SpindleRecipe::damage,
                SpindleRecipe::new);

        @Override
        public MapCodec<SpindleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SpindleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
