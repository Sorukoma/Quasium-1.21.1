package net.sorukoma.quasium.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
// import net.minecraft.nbt.CompoundTag; // Больше не нужен
// import net.sorukoma.quasium.component.ModDataComponents; // Больше не нужен

public class KaifometerItem extends Item {
    // public static final String KAIF_NBT_KEY = "KaifValue"; // Больше не нужен
    // public static final int MAX_KAIF = 100; // Больше не нужен

    public KaifometerItem(Properties pProperties) {
        super(pProperties);
    }

    // Методы getKaifValue и setKaifValue больше не нужны, так как кайф хранится в PlayerKaifData
    // public static int getKaifValue(ItemStack stack) { ... }
    // public static void setKaifValue(ItemStack stack, int value) { ... }

    // Метод onCraftedBy больше не нужен, так как кайф хранится в PlayerKaifData
    // @Override
    // public void onCraftedBy(ItemStack pStack, net.minecraft.world.level.Level pLevel, net.minecraft.world.entity.player.Player pPlayer) { ... }
}
