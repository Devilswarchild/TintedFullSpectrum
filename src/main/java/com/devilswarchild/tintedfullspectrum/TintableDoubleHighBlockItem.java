package com.devilswarchild.tintedfullspectrum;

import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

// Mirrors vanilla's own DoubleHighBlockItem (used for Items.TALL_GRASS etc.) -- extending it directly
// gets its "pre-clear whatever occupies the position above before placing" behavior for free, which
// plain TintableBlockItem (a bare BlockItem wrapper) doesn't have.
public class TintableDoubleHighBlockItem extends DoubleHighBlockItem implements TintableItem {
    public TintableDoubleHighBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
}
