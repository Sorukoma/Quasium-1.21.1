package net.sorukoma.quasium.selection.trade;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.selection.SelectionRegistry;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static net.sorukoma.quasium.selection.SelectionConfig.*;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class HookahMasterTrades {
    private static final List<Supplier<Item>> WILD_LEAVES = List.of(ModItems.DIKAR_LEAVES, ModItems.PESCHANIK_LEAVES,
            ModItems.BOLOTNIK_LEAVES, ModItems.LEDYANOY_LEAVES);
    private static final IntSupplier ONE = () -> 1;
    private static final float PRICE_MULTIPLIER = 0.05f;

    private HookahMasterTrades() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != SelectionRegistry.HOOKAH_MASTER.get()) return;
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        trades.get(1).addAll(List.of(
                new LeafBuy(LEAVES_BUY_COUNT, kind -> kind, ONE, 16, 2),
                tobacco(1, TOBACCO_COMMON_PRICE, 12, 1)));
        trades.get(2).addAll(List.of(
                new LeafBuy(DRIED_LEAVES_BUY_COUNT, kind -> kind.expect(ModDataComponents.DRYING_STAGE.get(), 2), DRIED_LEAVES_PRICE, 12, 10),
                tobacco(2, TOBACCO_COMMON_PRICE, 12, 5)));
        trades.get(3).addAll(List.of(
                new LeafBuy(THREE_STAR_LEAVES_COUNT, stars(3), THREE_STAR_PRICE, 12, 20),
                new OneOf(List.of(new Sale(Items.BLAZE_POWDER, BLAZE_POWDER_COUNT, BLAZE_POWDER_PRICE, 12, 10),
                        tobacco(3, TOBACCO_RARE_PRICE, 8, 10)))));
        trades.get(4).addAll(List.of(
                new LeafBuy(ONE, stars(4), FOUR_STAR_PRICE, 12, 30),
                new OneOf(List.of(new Sale(Items.CHORUS_FRUIT, CHORUS_FRUIT_COUNT, CHORUS_FRUIT_PRICE, 12, 15),
                        tobacco(4, TOBACCO_RARE_PRICE, 8, 15)))));
        trades.get(5).addAll(List.of(
                new LeafBuy(ONE, stars(5), FIVE_STAR_PRICE, 12, 30),
                new OneOf(List.of(new Sale(Items.ECHO_SHARD, ONE, ECHO_SHARD_PRICE, 3, 30),
                        tobacco(5, TOBACCO_RARE_PRICE, 4, 30)))));
    }

    private static VillagerTrades.ItemListing tobacco(int level, IntSupplier price, int maxUses, int xp) {
        return new OneOf(TobaccoShelf.atLevel(level).stream()
                .<VillagerTrades.ItemListing>map(pack -> new Sale(pack, ONE, price, maxUses, xp)).toList());
    }

    private static UnaryOperator<DataComponentPredicate.Builder> stars(int stars) {
        return kind -> kind.expect(SelectionRegistry.LEAF_QUALITY.get(), stars);
    }

    record LeafBuy(IntSupplier count, UnaryOperator<DataComponentPredicate.Builder> kind, IntSupplier price, int maxUses, int xp)
            implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            Item leaves = WILD_LEAVES.get(random.nextInt(WILD_LEAVES.size())).get();
            return new MerchantOffer(new ItemCost(leaves, count.getAsInt()).withComponents(kind),
                    new ItemStack(Items.EMERALD, price.getAsInt()), maxUses, xp, PRICE_MULTIPLIER);
        }
    }

    record Sale(Item item, IntSupplier count, IntSupplier price, int maxUses, int xp) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(new ItemCost(Items.EMERALD, price.getAsInt()), new ItemStack(item, count.getAsInt()),
                    maxUses, xp, PRICE_MULTIPLIER);
        }
    }

    record OneOf(List<VillagerTrades.ItemListing> options) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return options.get(random.nextInt(options.size())).getOffer(trader, random);
        }
    }
}
