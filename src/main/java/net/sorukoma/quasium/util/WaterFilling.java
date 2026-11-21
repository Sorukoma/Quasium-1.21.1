package net.sorukoma.quasium.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

public final class WaterFilling {
    public static final int BUCKET_ML = 1000;
    public static final int BOTTLE_ML = 250;

    private WaterFilling() {
    }

    public static int mlIn(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) return BUCKET_ML;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return stack.is(Items.POTION) && contents != null && contents.is(Potions.WATER) ? BOTTLE_ML : 0;
    }

    public static void pourOut(ItemStack stack, Player player, InteractionHand hand, Level level, BlockPos pos) {
        boolean bucket = stack.is(Items.WATER_BUCKET);
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(bucket ? Items.BUCKET : Items.GLASS_BOTTLE)));
        level.playSound(null, pos, bucket ? SoundEvents.BUCKET_EMPTY : SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1f, 1f);
    }
}
