package net.hyper.immortalcultivation.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.registry.ICModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record PillFurnaceRecipe(
    Ingredient inputItem1,
    Ingredient inputItem2,
    Ingredient inputItem3,
    ItemStack output,
    int input1Count,
    int input2Count,
    int input3Count,
    boolean requiresSpiritStone,
    double baseSuccessChance
) implements Recipe<PillFurnaceRecipeInput> {

    public ItemStack getResult() {
        return output.copy();
    }

    public int getInput1Count() {
        return input1Count;
    }

    public int getInput2Count() {
        return input2Count;
    }

    public int getInput3Count() {
        return input3Count;
    }

    public boolean requiresSpiritStone() {
        return requiresSpiritStone;
    }

    public double getBaseSuccessChance() {
        return baseSuccessChance;
    }

    @Override
    public boolean matches(PillFurnaceRecipeInput input, Level level) {
        if (level.isClientSide()) {
            return false;
        }

        // Check if each ingredient matches and has sufficient count
        boolean input1Matches = inputItem1.test(input.getItem(0)) &&
            input.getItem(0).getCount() >= input1Count;
        boolean input2Matches = inputItem2.isEmpty() ||
            (inputItem2.test(input.getItem(1)) &&
                input.getItem(1).getCount() >= input2Count);
        boolean input3Matches = inputItem3.isEmpty() ||
            (inputItem3.test(input.getItem(2)) &&
                input.getItem(2).getCount() >= input3Count);

        return input1Matches && input2Matches && input3Matches;
    }

    @Override
    public ItemStack assemble(PillFurnaceRecipeInput input, HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // Pill furnace doesn't use traditional crafting grid
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(inputItem1);
        if (!inputItem2.isEmpty()) ingredients.add(inputItem2);
        if (!inputItem3.isEmpty()) ingredients.add(inputItem3);
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ICModRecipes.PILL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ICModRecipes.PILL_FURNACE_TYPE.get();
    }

    // Recipe Serializer Implementation
    public static class Serializer implements RecipeSerializer<PillFurnaceRecipe> {

        public static final MapCodec<PillFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input1").forGetter(PillFurnaceRecipe::inputItem1),
                Ingredient.CODEC.optionalFieldOf("input2", Ingredient.EMPTY).forGetter(PillFurnaceRecipe::inputItem2),
                Ingredient.CODEC.optionalFieldOf("input3", Ingredient.EMPTY).forGetter(PillFurnaceRecipe::inputItem3),
                ItemStack.STRICT_CODEC.fieldOf("output").forGetter(PillFurnaceRecipe::output),
                Codec.INT.optionalFieldOf("input1_count", 1).forGetter(PillFurnaceRecipe::input1Count),
                Codec.INT.optionalFieldOf("input2_count", 1).forGetter(PillFurnaceRecipe::input2Count),
                Codec.INT.optionalFieldOf("input3_count", 1).forGetter(PillFurnaceRecipe::input3Count),
                Codec.BOOL.optionalFieldOf("requires_spirit_stone", true).forGetter(PillFurnaceRecipe::requiresSpiritStone),
                Codec.DOUBLE.optionalFieldOf("base_success_chance", 0.8).forGetter(PillFurnaceRecipe::baseSuccessChance)
            ).apply(instance, PillFurnaceRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, PillFurnaceRecipe> STREAM_CODEC = StreamCodec.of(
            Serializer::toNetwork,
            Serializer::fromNetwork
        );

        @Override
        public MapCodec<PillFurnaceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PillFurnaceRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static PillFurnaceRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            Ingredient input1 = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            Ingredient input2 = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            Ingredient input3 = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            int input1Count = buffer.readVarInt();
            int input2Count = buffer.readVarInt();
            int input3Count = buffer.readVarInt();
            boolean requiresSpiritStone = buffer.readBoolean();
            double baseSuccessChance = buffer.readDouble();

            return new PillFurnaceRecipe(input1, input2, input3, output,
                input1Count, input2Count, input3Count,
                requiresSpiritStone, baseSuccessChance);
        }

        public static void toNetwork(RegistryFriendlyByteBuf buffer, PillFurnaceRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.inputItem1);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.inputItem2);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.inputItem3);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.output);
            buffer.writeVarInt(recipe.input1Count);
            buffer.writeVarInt(recipe.input2Count);
            buffer.writeVarInt(recipe.input3Count);
            buffer.writeBoolean(recipe.requiresSpiritStone);
            buffer.writeDouble(recipe.baseSuccessChance);
        }
    }
}