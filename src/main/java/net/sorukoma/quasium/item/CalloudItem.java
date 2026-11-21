package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.screen.CalloudMenu;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CalloudItem extends Item {
    public CalloudItem(Properties properties) {
        super(properties);
    }

    public boolean burnsForever() {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            ModDataComponents.CalloudData contents = stack.getOrDefault(
                    ModDataComponents.CALLOUD_CONTENTS.get(),
                    new ModDataComponents.CalloudData(List.of(), List.of())
            );
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.quasium.calloud");
                }

                @Override
                public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new CalloudMenu(containerId, playerInventory, hand.ordinal(), contents);
                }
            }, buf -> {
                buf.writeVarInt(hand.ordinal());
                // Base charcoals
                buf.writeVarInt(contents.charcoals().size());
                for (ModDataComponents.CharcoalData cd : contents.charcoals()) {
                    buf.writeUtf(cd.size().name());
                    buf.writeVarInt(cd.remainingTicks());
                    buf.writeFloat(cd.heatingProgress());
                }
                // Stacked charcoals
                buf.writeVarInt(contents.stackedCharcoals().size());
                for (ModDataComponents.CharcoalData cd : contents.stackedCharcoals()) {
                    buf.writeUtf(cd.size().name());
                    buf.writeVarInt(cd.remainingTicks());
                    buf.writeFloat(cd.heatingProgress());
                }
            });
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ModDataComponents.CalloudData data = stack.getOrDefault(ModDataComponents.CALLOUD_CONTENTS.get(), new ModDataComponents.CalloudData(List.of(), List.of()));

        int volume = data.totalVolume();
        float heat = data.totalHeat();

        tooltipComponents.add(Component.translatable("quasium.tooltip.calloud_volume", volume, 12));
        tooltipComponents.add(Component.translatable("quasium.tooltip.calloud_heat", String.format("%.2f", heat)));

        if (!data.charcoals().isEmpty()) {
            tooltipComponents.add(Component.translatable("quasium.tooltip.calloud_charcoals", data.charcoals().size()));
        }
        if (!data.stackedCharcoals().isEmpty()) {
            tooltipComponents.add(Component.translatable("quasium.tooltip.calloud_stacked_charcoals", data.stackedCharcoals().size()));
        }
    }
}