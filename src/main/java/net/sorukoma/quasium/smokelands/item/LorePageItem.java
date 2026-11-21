package net.sorukoma.quasium.smokelands.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;

import java.util.List;

public class LorePageItem extends Item {
    public static final int PAGES = 6;

    public LorePageItem(Properties properties) {
        super(properties);
    }

    public static ItemStack page(int number) {
        ItemStack stack = new ItemStack(SmokelandsItems.LORE_PAGE.get());
        stack.set(SmokelandsItems.LORE_PAGE_NUMBER.get(), number);
        return stack;
    }

    public static int number(ItemStack stack) {
        return stack.getOrDefault(SmokelandsItems.LORE_PAGE_NUMBER.get(), 1);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.quasium.lore_page.numbered", number(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("quasium.progression.lore_" + number(stack) + ".title").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.quasium.lore_page.use").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            read(serverPlayer, number(stack));
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void read(ServerPlayer player, int number) {
        SmokecraftTrigger.fire(player, "lore_page_" + number);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.9f);
        player.displayClientMessage(Component.translatable("item.quasium.lore_page.read", number,
                Component.translatable("quasium.progression.lore_" + number + ".title")), true);
    }
}
