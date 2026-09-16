package com.devilswarchild.tintedfullspectrum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

// A real stonecutter recipe -- extends vanilla's own StonecutterRecipe (not just
// Recipe<SingleRecipeInput> directly) because StonecutterMenu hard-types its own recipe list to
// List<RecipeHolder<StonecutterRecipe>> specifically (confirmed via the real decompiled source), so
// anything that isn't actually a StonecutterRecipe instance would throw a ClassCastException the
// moment the stonecutter UI tried to populate its recipe list. Registers under vanilla's own
// RecipeType.STONECUTTING (inherited), so it works natively in the stonecutter UI and is
// enumerable/JEI-visible like any normal stonecutting recipe. The one actual difference from
// vanilla's own StonecutterRecipe: assemble() copies the input's TintColorComponent onto the result
// instead of vanilla's plain result.copy(), and matches() additionally requires the input to actually
// carry that component. Used for all six Tinted [Red] Sandstone -> Tinted [Red] Cut/Chiseled
// conversions (one Java class, six registered recipe JSON instances).
public class TintCarryingStonecuttingRecipe extends StonecutterRecipe {
    public TintCarryingStonecuttingRecipe(String group, Ingredient ingredient, ItemStack result) {
        super(group, ingredient, result);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item()) && input.item().has(TintedFullSpectrum.TINT_COLOR.get());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        TintColorComponent color = input.item().get(TintedFullSpectrum.TINT_COLOR.get());
        if (color == null) {
            return ItemStack.EMPTY;
        }
        ItemStack output = this.result.copy();
        output.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.TINT_CARRYING_STONECUTTING_SERIALIZER.get();
    }

    // Own serializer, not vanilla's SingleItemRecipe.Serializer -- that one's constructor is
    // `protected`, only usable from within net.minecraft.world.item.crafting itself (vanilla's own
    // STONECUTTER registration lives inside RecipeSerializer.java, in that same package). Mirrors its
    // codec shape exactly (group/ingredient/result).
    public static class Serializer implements RecipeSerializer<TintCarryingStonecuttingRecipe> {
        private static final MapCodec<TintCarryingStonecuttingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result))
                .apply(instance, TintCarryingStonecuttingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, TintCarryingStonecuttingRecipe> STREAM_CODEC = StreamCodec.composite(
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, r -> r.group,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
                ItemStack.STREAM_CODEC, r -> r.result,
                TintCarryingStonecuttingRecipe::new);

        @Override
        public MapCodec<TintCarryingStonecuttingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TintCarryingStonecuttingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
