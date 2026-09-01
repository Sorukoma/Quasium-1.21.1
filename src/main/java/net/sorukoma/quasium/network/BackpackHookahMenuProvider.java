package net.sorukoma.quasium.network;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.screen.BackpackHookahMenu;
import org.jetbrains.annotations.Nullable;

public class BackpackHookahMenuProvider implements MenuProvider {

    private final BackpackHookahState state;

    public BackpackHookahMenuProvider(BackpackHookahState state) {
        this.state = state;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.backpack_hookah");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BackpackHookahMenu(containerId, playerInventory, state);
    }
}