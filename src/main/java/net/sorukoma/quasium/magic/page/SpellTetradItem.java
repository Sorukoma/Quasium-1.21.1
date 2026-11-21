package net.sorukoma.quasium.magic.page;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.magic.page.form.ClassicPages;

import java.util.List;

public class SpellTetradItem extends Item {
    private final int pages;

    public SpellTetradItem(Properties properties, int pages) {
        super(properties.stacksTo(1).component(PageRegistry.TETRAD, firstPages(pages)));
        this.pages = pages;
    }

    private static TetradContents firstPages(int pages) {
        return pages > PageRegistry.SMALL_PAGES ? TetradContents.empty(pages) : ClassicPages.starter(pages);
    }

    public int pages() {
        return pages;
    }

    public static TetradContents contents(ItemStack stack) {
        if (!(stack.getItem() instanceof SpellTetradItem tetrad)) return TetradContents.empty(0);
        TetradContents contents = stack.getOrDefault(PageRegistry.TETRAD.get(), TetradContents.empty(tetrad.pages));
        return contents.size() == tetrad.pages ? contents : contents.resized(tetrad.pages);
    }

    public static void store(ItemStack stack, TetradContents contents) {
        stack.set(PageRegistry.TETRAD.get(), contents);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TetradContents contents = contents(stack);
        boolean written = false;
        for (int index = 0; index < contents.size(); index++) {
            SpellPage page = contents.page(index);
            if (page.isEmpty()) continue;
            written = true;
            boolean active = index == contents.active();
            Component label = PageLabels.of(PageCasting.label(page));
            tooltip.add(Component.translatable(active ? "quasium.tetrad.tooltip.active" : "quasium.tetrad.tooltip.page", index + 1, label)
                    .withStyle(active ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        }
        if (!written) tooltip.add(Component.translatable("quasium.tetrad.tooltip.empty").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("quasium.tetrad.tooltip.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
