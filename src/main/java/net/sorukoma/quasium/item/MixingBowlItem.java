package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.screen.MixingBowlMenu;
import org.jetbrains.annotations.Nullable;

public class MixingBowlItem extends Item {
    public MixingBowlItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);
        if (!pLevel.isClientSide()) {
            pPlayer.openMenu(new SimpleMenuProvider(
                    (pContainerId, pPlayerInventory, pPlayerEntity) -> new MixingBowlMenu(pContainerId, pPlayerInventory),
                    Component.translatable("container.quasium.mixing_bowl")
            ), buf -> {}); // Empty buffer writer for SimpleMenuProvider
        }
        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }
}