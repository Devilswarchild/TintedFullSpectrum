package com.devilswarchild.tintedfullspectrum;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

// A plain BlockItem that's also eligible for the generic recolor recipe -- for tintable blocks with
// no special placement item (Tinted Planks and its stairs/slab/fence variants).
public class TintableBlockItem extends BlockItem implements TintableItem {
    public TintableBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
}
