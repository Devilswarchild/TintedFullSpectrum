package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;

// Takes ANY vanilla planks/wood stairs/slab/fence/fence gate/door (any of the ~11 wood types) plus a
// Colored Dye, and outputs the equivalent Tinted shape in that dye's color -- one universal converter
// rather than needing a separate blank-dye step to obtain the mod's own plain shape first. Which
// vanilla item is "convertible" is determined by vanilla's own planks/wooden_stairs/wooden_slabs/
// wooden_fences/fence_gates/wooden_doors tags; which Tinted shape it maps to is determined by the
// input block's Java type (StairBlock/SlabBlock/FenceBlock/FenceGateBlock/DoorBlock, or plain Block
// for planks themselves), not the tag, since the tags don't distinguish shape on their own. Note the
// vanilla-parity Tinted Door maps here (plain door + dye, no extra ingredients) -- the Hourglass
// Door's door+glass-panes+dye recipe is a separate, differently-shaped recipe (see
// HourglassDoorConvertRecipe) since its window geometry actually calls for the glass panes.
public class ConvertAndDyeRecipe extends CustomRecipe {
    public ConvertAndDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] found = findInputs(input);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        TintColorComponent color = found[1].get(TintedFullSpectrum.TINT_COLOR.get());
        if (color == null) {
            return ItemStack.EMPTY;
        }
        Item output = tintedEquivalent(found[0].getItem());
        if (output == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(output);
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns {target, dye} if the grid holds exactly one convertible wood shape and one Colored Dye.
    private static ItemStack[] findInputs(CraftingInput input) {
        ItemStack target = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ColoredDyeItem) {
                if (!dye.isEmpty()) {
                    return null;
                }
                dye = stack;
            } else if (isConvertible(stack)) {
                if (!target.isEmpty()) {
                    return null;
                }
                target = stack;
            } else {
                return null;
            }
        }
        return !target.isEmpty() && !dye.isEmpty() ? new ItemStack[] {target, dye} : null;
    }

    private static boolean isConvertible(ItemStack stack) {
        return stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_STAIRS)
                || stack.is(ItemTags.WOODEN_SLABS) || stack.is(ItemTags.WOODEN_FENCES)
                || stack.is(ItemTags.FENCE_GATES) || stack.is(ItemTags.WOODEN_DOORS);
    }

    private static Item tintedEquivalent(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }
        Block block = blockItem.getBlock();
        if (block instanceof StairBlock) {
            return TintedFullSpectrum.TINTED_PLANKS_STAIRS_ITEM.get();
        } else if (block instanceof SlabBlock) {
            return TintedFullSpectrum.TINTED_PLANKS_SLAB_ITEM.get();
        } else if (block instanceof FenceGateBlock) {
            return TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE_ITEM.get();
        } else if (block instanceof FenceBlock) {
            return TintedFullSpectrum.TINTED_PLANKS_FENCE_ITEM.get();
        } else if (block instanceof DoorBlock) {
            return TintedFullSpectrum.TINTED_DOOR_ITEM.get();
        }
        return TintedFullSpectrum.TINTED_PLANKS_ITEM.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TintedFullSpectrum.CONVERT_AND_DYE_SERIALIZER.get();
    }
}
