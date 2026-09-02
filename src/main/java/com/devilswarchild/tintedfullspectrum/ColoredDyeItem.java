package com.devilswarchild.tintedfullspectrum;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

// Right-click a placed Colored Dye on any block whose block entity implements
// TintableBlockEntity (currently just the tinted torch) to apply its stored RGB. Applying to
// vanilla DYEABLE items (leather armor, etc.) instead goes through crafting -- see
// ColoredDyeApplyRecipe -- since that's how vanilla itself applies dyes to items, not right-click.
public class ColoredDyeItem extends Item {
    public ColoredDyeItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof TintableBlockEntity tintable)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            tintable.setColor(dyedColor.rgb());
            level.playSound(null, context.getClickedPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
