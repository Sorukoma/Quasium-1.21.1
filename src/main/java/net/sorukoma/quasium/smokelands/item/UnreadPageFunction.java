package net.sorukoma.quasium.smokelands.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class UnreadPageFunction extends LootItemConditionalFunction {
    public static final MapCodec<UnreadPageFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance -> commonFields(instance).apply(instance, UnreadPageFunction::new));

    private UnreadPageFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    public static LootItemConditionalFunction.Builder<?> unread() {
        return simpleBuilder(UnreadPageFunction::new);
    }

    @Override
    public LootItemFunctionType<UnreadPageFunction> getType() {
        return SmokelandsItems.UNREAD_PAGE.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        List<Integer> fresh = context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer opener
                ? fresh(opener) : List.of();
        RandomSource random = context.getRandom();
        int page = fresh.isEmpty() ? 1 + random.nextInt(LorePageItem.PAGES) : fresh.get(random.nextInt(fresh.size()));
        stack.set(SmokelandsItems.LORE_PAGE_NUMBER.get(), page);
        return stack;
    }

    private static List<Integer> fresh(ServerPlayer opener) {
        return IntStream.rangeClosed(1, LorePageItem.PAGES).filter(page -> !pasted(opener, page) && !carried(opener, page))
                .boxed().toList();
    }

    private static boolean pasted(ServerPlayer player, int page) {
        AdvancementHolder read = player.server.getAdvancements().get(SmokelandsDimension.id("smokelands/lore_" + page));
        return read != null && player.getAdvancements().getOrStartProgress(read).isDone();
    }

    private static boolean carried(ServerPlayer player, int page) {
        return player.getInventory().hasAnyMatching(stack -> stack.is(SmokelandsItems.LORE_PAGE.get()) && LorePageItem.number(stack) == page);
    }
}
