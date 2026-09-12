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

// Takes N vanilla planks/wood stairs/slab/fence/fence gate/door/torch/grass block/short grass/tall
// grass/wool/carpet (any of the ~11 wood types or 16 dye colors, plus the single-material items) plus
// a Colored Dye, and outputs N of the equivalent Tinted shape in that dye's color -- one universal
// converter rather than needing a separate blank-dye step to obtain the mod's own plain shape first.
// N is per-shape, not always 1: it matches that vanilla item's own real crafting yield (4 planks per
// log, 4 stairs, 6 slabs, 3 fence, 3 doors, 4 torches per craft) for shapes vanilla itself has no
// dyeing mechanic for at all, so a dyed batch matches what one crafting action of the base material
// would give you. Fence Gate stays 1:1 since vanilla's own recipe only ever yields 1. Wool and Carpet
// also stay 1:1, but for a different reason -- vanilla genuinely DOES dye those, 1-for-1
// (dye_white_wool.json et al), so that's not a gap to fill, it's already matched. Grass Block/Short
// Grass/Tall Grass stay 1:1 too since vanilla has no crafting recipe for them at all (silk
// touch/world gen only) -- no yield number exists to borrow.
//
// Which vanilla item is "convertible" is determined by vanilla's own planks/wooden_stairs/
// wooden_slabs/wooden_fences/fence_gates/wool/wool_carpets tags, or (for doors/torch/grass, which
// have no such convenient tag/type split) an exact Block check. Which Tinted shape it maps to, and
// the batch size, are both determined by the input block's Java type (StairBlock/SlabBlock/
// FenceBlock/FenceGateBlock/DoorBlock/CarpetBlock, or plain Block for planks/torch/grass/wool), not
// the tag, since the tags don't distinguish shape on their own (wool in particular needs a tag check
// here too, since its Block class is generic).
//
// Note the vanilla-parity Tinted Door and every single-material vanilla-parity item (Torch, Grass
// Block, Short Grass, Tall Grass, Wool, Carpet) all map here (plain vanilla item + dye, no extra
// ingredients, one step). The Hourglass Door and the original custom-geometry Tinted Torch are
// acquired completely differently -- a static/plain recipe makes a blank instance first, then the
// generic RecolorRecipe (Colored Dye + any TintableItem) colors it, a two-step pattern.
public class ConvertAndDyeRecipe extends CustomRecipe {
    public ConvertAndDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    // Bundles what a convertible Block maps to: the Tinted output item, and how many of the vanilla
    // input are required per craft (and how many of the output that craft produces -- always equal).
    private record Conversion(Item output, int batchSize) {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Object[] found = findInputs(input);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        ItemStack target = (ItemStack) found[0];
        ItemStack dye = (ItemStack) found[1];
        TintColorComponent color = dye.get(TintedFullSpectrum.TINT_COLOR.get());
        if (color == null) {
            return ItemStack.EMPTY;
        }
        Conversion conversion = conversionFor(target.getItem());
        if (conversion == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(conversion.output(), conversion.batchSize());
        result.set(TintedFullSpectrum.TINT_COLOR.get(), color);
        return result;
    }

    // Returns {target, dye} if the grid holds exactly one Colored Dye and exactly that shape's batch
    // size worth of ONE convertible item (all in separate cells, matching every other batch recipe in
    // this mod, e.g. WoolToCarpetRecipe/ChromaGlassPaneRecipe).
    private static Object[] findInputs(CraftingInput input) {
        ItemStack target = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;
        int targetCount = 0;
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
                if (!target.isEmpty() && stack.getItem() != target.getItem()) {
                    return null;
                }
                target = stack;
                targetCount++;
            } else {
                return null;
            }
        }
        if (target.isEmpty() || dye.isEmpty()) {
            return null;
        }
        Conversion conversion = conversionFor(target.getItem());
        return conversion != null && targetCount == conversion.batchSize() ? new Object[] {target, dye} : null;
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

    private static Conversion conversionFor(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }
        Block block = blockItem.getBlock();
        if (block == Blocks.TORCH) {
            // 1 coal/charcoal + 1 stick -> 4 torches, vanilla's own real yield.
            return new Conversion(TintedFullSpectrum.TINTED_VANILLA_TORCH_ITEM.get(), 4);
        } else if (block == Blocks.GRASS_BLOCK) {
            return new Conversion(TintedFullSpectrum.TINTED_GRASS_BLOCK_ITEM.get(), 1);
        } else if (block == Blocks.SHORT_GRASS) {
            return new Conversion(TintedFullSpectrum.TINTED_SHORT_GRASS_ITEM.get(), 1);
        } else if (block == Blocks.TALL_GRASS) {
            return new Conversion(TintedFullSpectrum.TINTED_TALL_GRASS_ITEM.get(), 1);
        } else if (block instanceof CarpetBlock) {
            // Vanilla really does dye carpet 1-for-1 (dye_white_carpet.json) -- not a gap to fill.
            return new Conversion(TintedFullSpectrum.TINTED_CARPET_ITEM.get(), 1);
        } else if (block.defaultBlockState().is(BlockTags.WOOL)) {
            // Same for wool (dye_white_wool.json) -- wool has no distinguishing Java class (just a
            // plain Block, all 16 colors), unlike carpet, so this has to be a tag check.
            return new Conversion(TintedFullSpectrum.TINTED_WOOL_ITEM.get(), 1);
        } else if (block instanceof StairBlock) {
            // 6 planks -> 4 stairs, vanilla's own real yield.
            return new Conversion(TintedFullSpectrum.TINTED_PLANKS_STAIRS_ITEM.get(), 4);
        } else if (block instanceof SlabBlock) {
            // 3 planks -> 6 slabs, vanilla's own real yield.
            return new Conversion(TintedFullSpectrum.TINTED_PLANKS_SLAB_ITEM.get(), 6);
        } else if (block instanceof FenceGateBlock) {
            // Vanilla's own fence gate recipe only ever yields 1 -- nothing to batch up to.
            return new Conversion(TintedFullSpectrum.TINTED_PLANKS_FENCE_GATE_ITEM.get(), 1);
        } else if (block instanceof FenceBlock) {
            // 4 planks + 2 sticks -> 3 fence, vanilla's own real yield.
            return new Conversion(TintedFullSpectrum.TINTED_PLANKS_FENCE_ITEM.get(), 3);
        } else if (block instanceof DoorBlock) {
            // Doors need a per-material lookup (16 variants, keeping each material's own grain)
            // instead of one universal output like every other shape here. 6 planks -> 3 doors,
            // vanilla's own real yield.
            String material = TintedFullSpectrum.VANILLA_DOOR_MATERIAL.get(block);
            var doorItem = material != null ? TintedFullSpectrum.TINTED_DOOR_ITEMS.get(material) : null;
            return doorItem != null ? new Conversion(doorItem.get(), 3) : null;
        }
        // Plain planks: 1 log -> 4 planks, vanilla's own real yield.
        return new Conversion(TintedFullSpectrum.TINTED_PLANKS_ITEM.get(), 4);
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
