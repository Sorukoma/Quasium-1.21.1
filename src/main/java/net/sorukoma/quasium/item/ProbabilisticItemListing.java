package net.sorukoma.quasium.item;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.BasicItemListing;
import org.jetbrains.annotations.Nullable;

/**
 * Обёртка над BasicItemListing с настраиваемой вероятностью появления.
 * getOffer() возвращает null при провале проверки — торг не появится у жителя.
 *
 * Вероятность настраивается в ModEvents.onVillagerTrades — ищите поля
 * *_TRADE_PROBABILITY для изменения шансов после тестирования.
 */
public class ProbabilisticItemListing implements ItemListing {
    private final BasicItemListing baseTrade;
    private final float probability;

    public ProbabilisticItemListing(BasicItemListing baseTrade, float probability) {
        this.baseTrade = baseTrade;
        this.probability = probability;
    }

    @Nullable
    @Override
    public MerchantOffer getOffer(Entity entity, RandomSource random) {
        if (random.nextFloat() < probability) {
            return baseTrade.getOffer(entity, random);
        }
        return null;
    }
}