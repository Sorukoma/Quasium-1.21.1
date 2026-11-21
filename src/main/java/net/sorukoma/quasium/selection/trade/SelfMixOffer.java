package net.sorukoma.quasium.selection.trade;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.quality.LeafQuality;

import java.util.List;
import java.util.Optional;

public final class SelfMixOffer extends MerchantOffer {
    private static final int MAX_USES = 4;

    public SelfMixOffer(int uses) {
        super(wantedPack(), Optional.empty(), new ItemStack(Items.EMERALD, SelectionConfig.CARAVAN_SELF_MIX_PRICE.get()),
                uses, MAX_USES, 1, 0f, 0);
    }

    public static MerchantOffer restore(MerchantOffer loaded) {
        boolean selfMix = !(loaded instanceof SelfMixOffer) && loaded.getItemCostA().item().value() == ModItems.SELF_MIXED_TOBACCO.get();
        return selfMix ? new SelfMixOffer(loaded.getUses()) : loaded;
    }

    public static boolean accepts(ItemStack pack) {
        return pack.is(ModItems.SELF_MIXED_TOBACCO.get())
                && LeafQuality.of(pack) >= SelectionConfig.CARAVAN_SELF_MIX_MIN_STARS.get()
                && pack.getOrDefault(ModDataComponents.TOBACCO_WEIGHT.get(), 0) >= SelectionConfig.CARAVAN_SELF_MIX_MIN_GRAMS.get();
    }

    @Override
    public boolean satisfiedBy(ItemStack first, ItemStack second) {
        return second.isEmpty() && accepts(first) && first.getCount() >= getCostA().getCount();
    }

    private static ItemCost wantedPack() {
        int stars = SelectionConfig.CARAVAN_SELF_MIX_MIN_STARS.get();
        int grams = SelectionConfig.CARAVAN_SELF_MIX_MIN_GRAMS.get();
        ItemLore terms = new ItemLore(List.of(Component.translatable("quasium.tooltip.caravan_offer", stars, grams)));
        return new ItemCost(ModItems.SELF_MIXED_TOBACCO.get(), 1).withComponents(wanted -> wanted
                .expect(SelectionRegistry.LEAF_QUALITY.get(), stars)
                .expect(ModDataComponents.TOBACCO_WEIGHT.get(), grams)
                .expect(DataComponents.LORE, terms));
    }
}
