package net.sorukoma.quasium.selection.trade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;

public class SmokeCaravanTrader extends WanderingTrader {
    public static final List<FlavorSource> FLAVOR_SOURCES = List.of(
            new FlavorSource(Items.APPLE, 4), new FlavorSource(Items.MELON_SLICE, 4), new FlavorSource(Items.GLOW_BERRIES, 4),
            new FlavorSource(Items.SUGAR, 8), new FlavorSource(Items.COCOA_BEANS, 4), new FlavorSource(Items.BLAZE_POWDER, 2),
            new FlavorSource(Items.PINK_PETALS, 4), new FlavorSource(Items.SWEET_BERRIES, 4), new FlavorSource(Items.HONEYCOMB, 2),
            new FlavorSource(Items.ICE, 4), new FlavorSource(Items.FERN, 4), new FlavorSource(Items.NETHER_WART, 4));
    private static final int BELL_INTERVAL_TICKS = 400;
    private static final String DEPARTS_AT = "QuasiumDepartsAt";
    private long departsAt;

    public record FlavorSource(Item item, int count) {
    }

    public SmokeCaravanTrader(EntityType<? extends SmokeCaravanTrader> type, Level level) {
        super(type, level);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        Random day = new Random(((ServerLevel) level()).getSeed() ^ level().getDayTime() / 24000L);
        List<FlavorSource> pool = new ArrayList<>(FLAVOR_SOURCES);
        Collections.shuffle(pool, day);
        int price = SelectionConfig.CARAVAN_FLAVOR_PRICE.get();
        int uses = SelectionConfig.CARAVAN_FLAVOR_MAX_USES.get();
        pool.stream().limit(SelectionConfig.CARAVAN_DAILY_FLAVORS.get()).forEach(source -> offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, price), new ItemStack(source.item(), source.count()), uses, 1, 0.05f)));
        List<Item> tobaccos = new ArrayList<>(TobaccoShelf.all());
        Collections.shuffle(tobaccos, day);
        int packs = SelectionConfig.CARAVAN_TOBACCO_MAX_USES.get();
        tobaccos.stream().limit(SelectionConfig.CARAVAN_TOBACCO_OFFERS.get()).forEach(tobacco -> offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, TobaccoShelf.price(tobacco)), new ItemStack(tobacco), packs, 1, 0.05f)));
        offers.add(new SelfMixOffer(0));
    }

    @Override
    public void openTradingScreen(Player player, Component displayName, int level) {
        OptionalInt menu = player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new CaravanTradeMenu(id, inventory, this), displayName));
        MerchantOffers offers = getOffers();
        if (menu.isPresent() && !offers.isEmpty()) {
            player.sendMerchantOffers(menu.getAsInt(), offers, level, getVillagerXp(), showProgressBar(), canRestock());
        }
    }

    public void stayFor(int ticks) {
        departsAt = level().getGameTime() + ticks;
        setDespawnDelay(ticks);
    }

    public long departsAt() {
        return departsAt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && departsAt > 0 && level().getGameTime() >= departsAt && !isTrading()) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong(DEPARTS_AT, departsAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        departsAt = tag.getLong(DEPARTS_AT);
        if (offers != null) offers.replaceAll(SelfMixOffer::restore);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        playSound(ModSoundEvents.CARAVAN_BELL.get(), 1.0f, 1.0f);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isTrading() ? super.getAmbientSound() : ModSoundEvents.CARAVAN_BELL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return BELL_INTERVAL_TICKS;
    }
}
