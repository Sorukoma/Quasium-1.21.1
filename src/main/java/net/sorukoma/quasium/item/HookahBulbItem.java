package net.sorukoma.quasium.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.hookah.HookahBulbVariant;

/**
 * Колба модульного кальяна. Ставится как блок-основа; вариант (цвет) переносится
 * в BlockEntity через DataComponent HOOKAH_BULB_VARIANT.
 */
public class HookahBulbItem extends BlockItem {
    private final HookahBulbVariant variant;

    public HookahBulbItem(Block block, HookahBulbVariant variant, Properties properties) {
        super(block, properties);
        this.variant = variant;
    }

    public HookahBulbVariant getVariant() {
        return variant;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.set(ModDataComponents.HOOKAH_BULB_VARIANT.get(), variant.getName());
        return stack;
    }
}
