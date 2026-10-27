package com.koenigstag.pull_cart;

import com.mojang.serialization.Codec;
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
 * {@code pull_cart:spinning}: what the loom spins, one item at a time.
 * <pre>
 * {"type": "pull_cart:spinning", "ingredient": {"tag": "minecraft:wool"}, "result": {"id": "minecraft:string", "count": 4},
 *  "experience": 2.0, "spinning_time": 400}
 * </pre>
 * Experience is for each item spun, and is given when you take the result out; spinning_time is in ticks (400 if left
 * out).
 */
public record SpinningRecipe(Ingredient ingredient, ItemStack result, float experience, int spinningTime)
        implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_SPINNING_TIME = 400;

    /** The recipe that spins this item, or null. */
    @Nullable
    public static SpinningRecipe find(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        return level.getRecipeManager().getRecipeFor(PullCartMod.SPINNING.get(), new SingleRecipeInput(input), level)
                .map(RecipeHolder::value).orElse(null);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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
    public RecipeSerializer<?> getSerializer() {
        return PullCartMod.SPINNING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return PullCartMod.SPINNING.get();
    }

    public static final class Serializer implements RecipeSerializer<SpinningRecipe> {
        private static final MapCodec<SpinningRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SpinningRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SpinningRecipe::result),
                Codec.FLOAT.optionalFieldOf("experience", 0f).forGetter(SpinningRecipe::experience),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("spinning_time", DEFAULT_SPINNING_TIME).forGetter(SpinningRecipe::spinningTime)
        ).apply(instance, SpinningRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, SpinningRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SpinningRecipe::ingredient,
                ItemStack.STREAM_CODEC, SpinningRecipe::result,
                ByteBufCodecs.FLOAT, SpinningRecipe::experience,
                ByteBufCodecs.VAR_INT, SpinningRecipe::spinningTime,
                SpinningRecipe::new);

        @Override
        public MapCodec<SpinningRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SpinningRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
