package com.devilswarchild.tintedfullspectrum;

import net.minecraft.world.item.Item;

// A plain, non-block tintable item -- for raw/intermediate materials that carry a color but never
// place a block of their own (Tinted Clay Ball, Tinted Brick). Same role TintableBlockItem plays for
// block-placing items, just without the BlockItem half.
public class TintableRawItem extends Item implements TintableItem {
    public TintableRawItem(Item.Properties properties) {
        super(properties);
    }
}
