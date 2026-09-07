package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;

// Takes ANY vanilla planks/wood stairs/slab/fence/fence gate/door/torch/grass block/short grass/tall
// grass/wool/carpet (any of the ~11 wood types or 16 dye colors, plus the single-material items) plus
// a Colored Dye, and outputs the equivalent Tinted shape in that dye's color -- one universal
// converter rather than needing a separate blank-dye step to obtain the mod's own plain shape first.
// Which vanilla item is "convertible" is determined by vanilla's own planks/wooden_stairs/
// wooden_slabs/wooden_fences/fence_gates/wool/wool_carpets tags, or (for doors/torch/grass, which
// have no such convenient tag/type split) an exact Block check. Which Tinted shape it maps to is
// determined by the input block's Java type (StairBlock/SlabBlock/FenceBlock/FenceGateBlock/
// DoorBlock/CarpetBlock, or plain Block for planks/torch/grass/wool), not the tag, since the tags
// don't distinguish shape on their own (wool in particular needs a tag check here too, since its
// Block class is generic). Note the vanilla-parity Tinted Door and every single-material
// vanilla-parity item (Torch, Grass Block, Short Grass, Tall Grass, Wool, Carpet) all map here (plain
// vanilla item + dye, no extra ingredients, one step). The Hourglass Door and the original
// custom-geometry Tinted Torch are acquired completely differently -- a static/plain recipe makes a
// blank instance first, then the generic RecolorRecipe (Colored Dye + any TintableItem) colors it, a
// two-step pattern.
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
        if (stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_STAIRS)
                || stack.is(ItemTags.WOODEN_SLABS) || stack.is(ItemTags.WOODEN_FENCES)
                || stack.is(ItemTags.FENCE_GATES) || stack.is(ItemTags.WOOL) || stack.is(ItemTags.WOOL_CARPETS)) {
            return true;
        }
        // Vanilla's torch item is a BlockItem whose getBlock() is always Blocks.TORCH (the floor
        // form) -- StandingAndWallBlockItem picks the wall block at placement time, not at the item
        // level, so this single check covers both. Only one material exists here (unlike doors), so
        // there's no need for a VANILLA_DOOR_MATERIAL-style lookup map, just this direct check.
        if (stack.getItem() instanceof BlockItem torchItem && torchItem.getBlock() == Blocks.TORCH) {
            return true;
        }
        // Grass Block / Short Grass / Tall Grass -- same single-material direct-check pattern as the
        // torch (each is its own standalone convertible, no per-species map needed).
        if (stack.getItem() instanceof BlockItem grassItem
                && (grassItem.getBlock() == Blocks.GRASS_BLOCK || grassItem.getBlock() == Blocks.SHORT_GRASS
                        || grassItem.getBlock() == Blocks.TALL_GRASS)) {
            return true;
        }
        // Doors are convertible only if they're an exact match in VANILLA_DOOR_MATERIAL (not just
        // "any door" via ItemTags.DOORS) -- waxed copper doors are deliberately excluded there so
        // this recipe cleanly doesn't match for them, rather than matching and then producing an
        // empty/confusingly-named result. See the map's own comment for why.
        return stack.getItem() instanceof BlockItem blockItem
                && TintedFullSpectrum.VANILLA_DOOR_MATERIAL.containsKey(blockItem.getBlock());
    }

    private static Item tintedEquivalent(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }
        Block block = blockItem.getBlock();
        if (block == Blocks.TORCH) {
            return TintedFullSpectrum.TINTED_VANILLA_TORCH_ITEM.get();
        } else if (block == Blocks.GRASS_BLOCK) {
            return TintedFullSpectrum.TINTED_GRASS_BLOCK_ITEM.get();
        } else if (block == Blocks.SHORT_GRASS) {
            return TintedFullSpectrum.TINTED_SHORT_GRASS_ITEM.get();
        } else if (block == Blocks.TALL_GRASS) {
            return TintedFullSpectrum.TINTED_TALL_GRASS_ITEM.get();
        } else if (block instanceof CarpetBlock) {
            // Checked before the WOOL tag below since carpet isn't in that tag, but the order doesn't
            // actually matter -- the two tags are mutually exclusive.
            return TintedFullSpectrum.TINTED_CARPET_ITEM.get();
        } else if (block.defaultBlockState().is(BlockTags.WOOL)) {
            // Wool has no distinguishing Java class (just a plain Block, all 16 colors), unlike
            // carpet -- has to be a tag check instead of instanceof.
            return TintedFullSpectrum.TINTED_WOOL_ITEM.get();
        } else if (block instanceof StairBlock) {
            return TintedFullSpectrum.TINTED_PLANKS_STAIRS_ITEM.get();
        } else if (block instanceof SlabBlock) {
            return TintedFullSpectrum.TINTED_PLANKS_SLAB_ITEM.get();
        } else if (block instanceof FenceGateBlock) {
            return TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE_ITEM.get();
        } else if (block instanceof FenceBlock) {
            return TintedFullSpectrum.TINTED_PLANKS_FENCE_ITEM.get();
        } else if (block instanceof DoorBlock) {
            // Doors need a per-material lookup (16 variants, keeping each material's own grain)
            // instead of one universal output like every other shape here.
            String material = TintedFullSpectrum.VANILLA_DOOR_MATERIAL.get(block);
            var doorItem = material != null ? TintedFullSpectrum.TINTED_DOOR_ITEMS.get(material) : null;
            return doorItem != null ? doorItem.get() : null;
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
