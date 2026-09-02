package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

// StandingAndWallBlockItem (auto-places floor vs wall form, like vanilla's torch item) that's also
// eligible for the generic recolor recipe. Used for the tinted torch.
public class TintableStandingAndWallBlockItem extends StandingAndWallBlockItem implements TintableItem {
    public TintableStandingAndWallBlockItem(Block block, Block wallBlock, Item.Properties properties, Direction attachmentDirection) {
        super(block, wallBlock, properties, attachmentDirection);
    }
}
