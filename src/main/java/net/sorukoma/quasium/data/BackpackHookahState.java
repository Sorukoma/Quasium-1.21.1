package net.sorukoma.quasium.data;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.item.CalloudItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.PacketBackpackHookahPuffData;
import net.sorukoma.quasium.network.PacketSpawnSmoke;
import net.sorukoma.quasium.network.PacketStopHookahPuffSoundClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BackpackHookahState {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int TICKS_PER_SECOND = 20;
    public static final int TOBACCO_TICKS_PER_GRAM = 320;
    public static final int MAX_TOBACCO_WEIGHT = 20 * TOBACCO_TICKS_PER_GRAM;
    public static final int TOBACCO_CONSUMPTION_PER_PUFF = 1;
    public static final int TOBACCO_OVER_BURN_THRESHOLD_GRAMS = 15;
    public static final float TOBACCO_OVER_BURN_EXTRA_RATE = 0.5f;
    public static final int AIR_CONSUMPTION_PER_TICK_PUFF = 10;

    // Пороги силы жара для разных типов табака
    public static final float HEAT_THRESHOLD_REGULAR = 1.0f;
    public static final float HEAT_THRESHOLD_SELF_MIXED_TOBACCO = 1.5f;
    public static final float HEAT_THRESHOLD_SELF_MIXED_MAKHORKA = 2.0f;

    // Лимиты углей для перегрева
    public static final int CHARCOAL_LIMIT_REGULAR = 2;
    public static final int CHARCOAL_LIMIT_SELF_MIXED_TOBACCO = 3;
    public static final int CHARCOAL_LIMIT_SELF_MIXED_MAKHORKA = 4;

    // За каждый уголь сверх лимита — x3 к сгоранию табака
    public static final float OVERHEAT_MULTIPLIER_PER_CHARCOAL = 3.0f;

    // Остывание heatingProgress углей в каллауде при idle — множитель замедления
    // В печке без топлива: coolingTime = maxTicks/3*2. В каллауде: coolingTime = maxTicks * factor
    // factor=5 → MM_27 остывает за 10 мин, MM_25 за 7.5 мин, MM_22 за 5 мин
    public static final int CALLOUD_IDLE_COOLING_FACTOR = 5;

    // Перегрев: если totalHeat > threshold × 1.8 (жара на 80% больше чем требуется), табак сгорает быстрее
    // rate = excessHeat × 0.4 за тик. 2 MM_27 при 100% для обычного табака: excessHeat=1.2, rate=0.48/тик
    public static final float OVERHEAT_THRESHOLD_MULTIPLIER = 1.8f;
    public static final float OVERHEAT_BURN_RATE_PER_UNIT = 0.4f;

    private static final String NBT_KEY = "QuasiumBackpackHookah";

    private int totalTobaccoWeight = 0;
    @Nullable
    private ModDataComponents.HookahBowlContents cachedBowlContents = null;
    private int lastGramsCount = 0;
    private float tobaccoOverBurnAccumulator = 0f;
    private float overheatBurnAccumulator = 0f;

    private boolean isPuffing = false;
    private int puffDurationTicks = 0;
    private int airConsumed = 0;
    private int kaifAccumulator = 0;
    private float puffEfficiencyAccumulator = 0f;

    private boolean needsSync = false;

    // Не final: на респауне/смене измерения создаётся новый ServerPlayer,
    // ссылку нужно обновлять, иначе тик пишет в мёртвый объект.
    private ServerPlayer player;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot == 1) {
                if (stack.isEmpty()) {
                    super.setStackInSlot(slot, ItemStack.EMPTY);
                    totalTobaccoWeight = 0;
                    tobaccoOverBurnAccumulator = 0f;
                    cachedBowlContents = null;
                    lastGramsCount = 0;
                    needsSync = true;
                } else if (stack.is(ModItems.HOOKAH_BOWL.get())) {
                    super.setStackInSlot(slot, stack.copyWithCount(1));
                    readBowlContents(itemHandler.getStackInSlot(1));
                    needsSync = true;
                } else {
                    return;
                }
                return;
            }
            super.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 1 ? 1 : super.getSlotLimit(slot);
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return slot == 1 ? 1 : super.getStackLimit(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (player != null && !player.level().isClientSide()) {
                needsSync = true;
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) {
                return stack.getItem() instanceof CalloudItem;
            } else if (slot == 1) {
                return stack.is(ModItems.HOOKAH_BOWL.get());
            }
            return false;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot == 1) {
                if (!isItemValid(slot, stack)) return stack;
                if (!getStackInSlot(slot).isEmpty()) return stack;
                if (!simulate) {
                    super.setStackInSlot(slot, stack.copyWithCount(1));
                    readBowlContents(itemHandler.getStackInSlot(1));
                    needsSync = true;
                }
                return ItemStack.EMPTY;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 1) {
                ItemStack current = getStackInSlot(slot);
                if (current.isEmpty()) return ItemStack.EMPTY;
                if (!simulate) {
                    totalTobaccoWeight = 0;
                    tobaccoOverBurnAccumulator = 0f;
                    cachedBowlContents = null;
                    lastGramsCount = 0;
                    super.setStackInSlot(slot, ItemStack.EMPTY);
                    needsSync = true;
                    return current.copy();
                }
                return current.copy();
            }
            return super.extractItem(slot, amount, simulate);
        }
    };

    public BackpackHookahState(ServerPlayer player) {
        this.player = player;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isHookahContent(ItemStack stack) {
        return itemHandler.isItemValid(1, stack);
    }

    // ─── Heat helpers ───

    public float getTotalHeat() {
        ItemStack calloudStack = itemHandler.getStackInSlot(0);
        if (!(calloudStack.getItem() instanceof CalloudItem)) return 0f;
        ModDataComponents.CalloudData calloudData = calloudStack.getOrDefault(
                ModDataComponents.CALLOUD_CONTENTS.get(),
                new ModDataComponents.CalloudData(List.of(), List.of())
        );
        return calloudData.totalHeat();
    }

    public int getCharcoalCount() {
        ItemStack calloudStack = itemHandler.getStackInSlot(0);
        if (!(calloudStack.getItem() instanceof CalloudItem)) return 0;
        ModDataComponents.CalloudData calloudData = calloudStack.getOrDefault(
                ModDataComponents.CALLOUD_CONTENTS.get(),
                new ModDataComponents.CalloudData(List.of(), List.of())
        );
        return calloudData.charcoalCount();
    }

    /** Определить тип табака и вернуть порог жара. */
    private float getHeatThreshold() {
        if (cachedBowlContents == null || cachedBowlContents.groups().isEmpty()) {
            return HEAT_THRESHOLD_REGULAR;
        }
        boolean hasSelfMixed = false;
        boolean isMakhorka = false;
        for (ModDataComponents.BowlTobaccoGroup group : cachedBowlContents.groups()) {
            if (group.isSelfMixed()) {
                hasSelfMixed = true;
                if ("makhorka".equals(group.base())) {
                    isMakhorka = true;
                }
            }
        }
        if (isMakhorka) return HEAT_THRESHOLD_SELF_MIXED_MAKHORKA;
        if (hasSelfMixed) return HEAT_THRESHOLD_SELF_MIXED_TOBACCO;
        return HEAT_THRESHOLD_REGULAR;
    }

    /** Определить лимит углей для текущего табака. */
    private int getCharcoalLimit() {
        if (cachedBowlContents == null || cachedBowlContents.groups().isEmpty()) {
            return CHARCOAL_LIMIT_REGULAR;
        }
        boolean hasSelfMixed = false;
        boolean isMakhorka = false;
        for (ModDataComponents.BowlTobaccoGroup group : cachedBowlContents.groups()) {
            if (group.isSelfMixed()) {
                hasSelfMixed = true;
                if ("makhorka".equals(group.base())) {
                    isMakhorka = true;
                }
            }
        }
        if (isMakhorka) return CHARCOAL_LIMIT_SELF_MIXED_MAKHORKA;
        if (hasSelfMixed) return CHARCOAL_LIMIT_SELF_MIXED_TOBACCO;
        return CHARCOAL_LIMIT_REGULAR;
    }

    private void readBowlContents(ItemStack bowlItem) {
        ModDataComponents.HookahBowlContents contents = bowlItem.getOrDefault(
                ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                ModDataComponents.HookahBowlContents.cleared());
        if (!contents.slotAssignments().isEmpty() || !contents.burnedSlots().isEmpty()) {
            contents = contents.withHeated(true);
            bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), contents);
            totalTobaccoWeight = contents.filledSlotCount() * TOBACCO_TICKS_PER_GRAM;
            cachedBowlContents = contents;
            lastGramsCount = contents.filledSlotCount();
        } else {
            totalTobaccoWeight = 0;
            tobaccoOverBurnAccumulator = 0f;
            cachedBowlContents = null;
            lastGramsCount = 0;
        }
    }

    private void updateBowlItemDuringConsumption() {
        ItemStack bowlItem = itemHandler.getStackInSlot(1);
        if (bowlItem.isEmpty() || !bowlItem.is(ModItems.HOOKAH_BOWL.get())) return;

        int currentGrams = totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM;
        if (currentGrams == lastGramsCount) return;

        int slotsToBurn = lastGramsCount - currentGrams;
        ModDataComponents.HookahBowlContents contents = bowlItem.getOrDefault(
                ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                new ModDataComponents.HookahBowlContents(List.of(), Map.of(), Set.of(), false));

        for (int i = 0; i < slotsToBurn; i++) {
            contents = contents.burnLastSlot();
        }

        if (contents.slotAssignments().isEmpty() && contents.burnedSlots().isEmpty()) {
            contents = contents.cleared();
        }

        bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), contents);

        if (!contents.slotAssignments().isEmpty() || !contents.burnedSlots().isEmpty()) {
            cachedBowlContents = contents;
        } else {
            cachedBowlContents = null;
        }

        lastGramsCount = currentGrams;
    }

    public void startPuffing() {
        this.isPuffing = true;
        this.puffDurationTicks = 0;
        this.airConsumed = 0;
        this.kaifAccumulator = 0;
        this.puffEfficiencyAccumulator = 0f;
        needsSync = true;
    }

    public void stopPuffing() {
        if (!isPuffing) return;
        this.isPuffing = false;

        // Compute effective duration from average puff efficiency
        int effectivePuffDuration = 0;
        if (puffDurationTicks > 0) {
            float avgPuffEfficiency = puffEfficiencyAccumulator / puffDurationTicks;
            effectivePuffDuration = (int)(puffDurationTicks * avgPuffEfficiency);
        }

        if (effectivePuffDuration > 0 && cachedBowlContents != null) {
            applyTobaccoEffects(player, effectivePuffDuration, cachedBowlContents);
            for (ModDataComponents.BowlTobaccoGroup group : cachedBowlContents.groups()) {
                if (group.isRegular()) {
                    PlayerTobaccoData.addTriedFlavors(player, Set.of(group.flavor()));
                } else {
                    PlayerTobaccoData.addTriedFlavors(player, Set.of("self_mixed_" + group.base()));
                }
            }
        }

        PacketDistributor.sendToPlayer(player, new PacketStopHookahPuffSoundClient(null, true));
        net.sorukoma.quasium.network.ModPackets.sendToClientsTrackingEntityAndSelf(new PacketSpawnSmoke(player.getId(), player.getLookAngle()), player);

        needsSync = true;
    }

    public boolean isPuffing() {
        return isPuffing;
    }

    public boolean tryPuff() {
        if (player.getAirSupply() <= AIR_CONSUMPTION_PER_TICK_PUFF) {
            ItemStack pipeItem = player.getMainHandItem().is(ModItems.HOOKAH_PIPE.get()) ?
                    player.getMainHandItem() : player.getOffhandItem();
            if (pipeItem.is(ModItems.HOOKAH_PIPE.get())) {
                long cooldownEndTime = player.level().getGameTime() + 100L;
                pipeItem.set(ModDataComponents.HOOKAH_PIPE_COOLDOWN_END.get(), cooldownEndTime);
            }
            stopPuffing();
            return false;
        }

        boolean hasPuffableContent = totalTobaccoWeight > 0;

        // Calloud & heat check
        ItemStack calloudStack = itemHandler.getStackInSlot(0);
        if (!(calloudStack.getItem() instanceof CalloudItem)) {
            stopPuffing();
            return false;
        }

        ModDataComponents.CalloudData calloudData = calloudStack.getOrDefault(
                ModDataComponents.CALLOUD_CONTENTS.get(),
                new ModDataComponents.CalloudData(List.of(), List.of())
        );

        float totalHeat = calloudData.totalHeat();
        float heatThreshold = getHeatThreshold();
        int charcoalCount = calloudData.charcoalCount();
        int charcoalLimit = getCharcoalLimit();

        // Not enough heat — can't puff
        if (totalHeat < heatThreshold || !hasPuffableContent) {
            stopPuffing();
            return false;
        }

        // Puff efficiency: if heatRatio < 60% of max possible heat, effects scale down
        float maxPossibleHeat = calloudData.maxPossibleHeat();
        float heatRatio = maxPossibleHeat > 0f ? totalHeat / maxPossibleHeat : 0f;
        final float puffEfficiency = heatRatio >= 0.6f ? 1.0f : (heatRatio / 0.6f) * 0.8f;

        // Overheat multiplier: for each charcoal beyond limit, multiply tobacco consumption by 3
        float overheatMultiplier = 1.0f;
        if (charcoalCount > charcoalLimit) {
            int extra = charcoalCount - charcoalLimit;
            overheatMultiplier = (float) Math.pow(OVERHEAT_MULTIPLIER_PER_CHARCOAL, extra);
        }

        // Decay each charcoal's remaining ticks (1 tick of puff = 1 tick of burn)
        List<ModDataComponents.CharcoalData> newCharcoals = new ArrayList<>();
        boolean anyRemoved = false;
        for (ModDataComponents.CharcoalData cd : calloudData.charcoals()) {
            int newRemaining = Math.max(0, cd.remainingTicks() - 1);
            if (newRemaining > 0) {
                newCharcoals.add(new ModDataComponents.CharcoalData(cd.size(), newRemaining, cd.heatingProgress()));
            } else {
                anyRemoved = true;
            }
        }

        // Decay stacked charcoals remaining ticks too
        List<ModDataComponents.CharcoalData> newStackedCharcoals = new ArrayList<>();
        for (ModDataComponents.CharcoalData cd : calloudData.stackedCharcoals()) {
            int newRemaining = Math.max(0, cd.remainingTicks() - 1);
            if (newRemaining > 0) {
                newStackedCharcoals.add(new ModDataComponents.CharcoalData(cd.size(), newRemaining, cd.heatingProgress()));
            } else {
                anyRemoved = true;
            }
        }

        if (anyRemoved || newCharcoals.size() != calloudData.charcoals().size() || newStackedCharcoals.size() != calloudData.stackedCharcoals().size()) {
            calloudStack.set(ModDataComponents.CALLOUD_CONTENTS.get(), new ModDataComponents.CalloudData(newCharcoals, newStackedCharcoals));
            needsSync = true;
        }

        if (totalTobaccoWeight > 0) {
            int consumption = TOBACCO_CONSUMPTION_PER_PUFF;
            int currentGrams = totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM;
            int burnedSlotsCount = cachedBowlContents != null ? cachedBowlContents.burnedSlots().size() : 0;
            int totalOccupiedSlots = currentGrams + burnedSlotsCount;
            if (totalOccupiedSlots > TOBACCO_OVER_BURN_THRESHOLD_GRAMS) {
                tobaccoOverBurnAccumulator += TOBACCO_OVER_BURN_EXTRA_RATE;
                if (tobaccoOverBurnAccumulator >= 1.0f) {
                    consumption += 1;
                    tobaccoOverBurnAccumulator -= 1.0f;
                }
            } else {
                tobaccoOverBurnAccumulator = 0f;
            }

            // Overheat burn: if totalHeat > threshold × 1.8, tobacco burns faster
            float overheatThreshold = heatThreshold * OVERHEAT_THRESHOLD_MULTIPLIER;
            if (totalHeat > overheatThreshold) {
                float excessHeat = totalHeat - overheatThreshold;
                overheatBurnAccumulator += excessHeat * OVERHEAT_BURN_RATE_PER_UNIT;
                if (overheatBurnAccumulator >= 1.0f) {
                    consumption += 1;
                    overheatBurnAccumulator -= 1.0f;
                }
            } else {
                overheatBurnAccumulator = 0f;
            }

            // Apply overheat multiplier
            consumption = (int) Math.ceil(consumption * overheatMultiplier);

            totalTobaccoWeight -= consumption;
            if (totalTobaccoWeight < 0) totalTobaccoWeight = 0;

            if (consumption > 0) {
                PlayerOverdoseData.recordConsumption(player, consumption);
            }

            if (totalTobaccoWeight == 0) {
                lastGramsCount = 0;

                ItemStack bowlItem = itemHandler.getStackInSlot(1);
                if (!bowlItem.isEmpty() && bowlItem.is(ModItems.HOOKAH_BOWL.get())) {
                    ModDataComponents.HookahBowlContents remainingContents = bowlItem.getOrDefault(
                            ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                            new ModDataComponents.HookahBowlContents(List.of(), Map.of(), Set.of(), false));
                    if (remainingContents.slotAssignments().isEmpty() && remainingContents.burnedSlots().isEmpty()) {
                        bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), remainingContents.cleared());
                        cachedBowlContents = null;
                    } else {
                        bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), remainingContents);
                        cachedBowlContents = remainingContents;
                    }
                } else {
                    cachedBowlContents = null;
                }
                needsSync = true;
            } else {
                updateBowlItemDuringConsumption();
                needsSync = true;
            }
        }

        puffDurationTicks++;
        airConsumed += AIR_CONSUMPTION_PER_TICK_PUFF;
        puffEfficiencyAccumulator += puffEfficiency;
        player.setAirSupply(Math.max(0, player.getAirSupply() - AIR_CONSUMPTION_PER_TICK_PUFF));

        // Kaif — check compatibility
        boolean isCompatible = cachedBowlContents != null && cachedBowlContents.isCompatible();

        kaifAccumulator++;
        if (kaifAccumulator >= TICKS_PER_SECOND) {
            int kaifPerSecond = Config.KAIF_PER_SECOND_SMOKING.getAsInt();
            if (kaifPerSecond > 0) {
                int adjustedKaif = Math.max(1, (int)(kaifPerSecond * puffEfficiency));
                if (isCompatible) {
                    PlayerKaifData.addKaif(player, adjustedKaif);
                } else {
                    PlayerKaifData.subtractKaif(player, kaifPerSecond);
                    applyNauseaFromNegativeKaif();
                }
            }
            kaifAccumulator = 0;
        }

        return true;
    }

    public void tick() {
        // Idle coal decay: when not puffing, coals cool down slowly (remainingTicks stays — consistent with heater)
        if (!isPuffing) {
            ItemStack calloudStack = itemHandler.getStackInSlot(0);
            if (calloudStack.getItem() instanceof CalloudItem) {
                ModDataComponents.CalloudData calloudData = calloudStack.getOrDefault(
                        ModDataComponents.CALLOUD_CONTENTS.get(),
                        new ModDataComponents.CalloudData(List.of(), List.of())
                );
                if (!calloudData.charcoals().isEmpty() || !calloudData.stackedCharcoals().isEmpty()) {
                    List<ModDataComponents.CharcoalData> newCharcoals = new ArrayList<>();
                    List<ModDataComponents.CharcoalData> newStackedCharcoals = new ArrayList<>();
                    boolean anyChanged = false;

                    for (ModDataComponents.CharcoalData cd : calloudData.charcoals()) {
                        float newProgress = cd.heatingProgress();
                        if (newProgress > 0f) {
                            float coolingPerTick = 100.0f / (cd.size().getMaxTicks() * CALLOUD_IDLE_COOLING_FACTOR);
                            newProgress = Math.max(0f, newProgress - coolingPerTick);
                        }

                        if (cd.remainingTicks() > 0 || newProgress > 0f) {
                            if (newProgress != cd.heatingProgress()) {
                                anyChanged = true;
                            }
                            newCharcoals.add(new ModDataComponents.CharcoalData(cd.size(), cd.remainingTicks(), newProgress));
                        } else {
                            anyChanged = true;
                        }
                    }

                    // Stacked charcoals cool down the same way as base
                    for (ModDataComponents.CharcoalData cd : calloudData.stackedCharcoals()) {
                        float newProgress = cd.heatingProgress();
                        if (newProgress > 0f) {
                            float coolingPerTick = 100.0f / (cd.size().getMaxTicks() * CALLOUD_IDLE_COOLING_FACTOR);
                            newProgress = Math.max(0f, newProgress - coolingPerTick);
                        }

                        if (cd.remainingTicks() > 0 || newProgress > 0f) {
                            if (newProgress != cd.heatingProgress()) {
                                anyChanged = true;
                            }
                            newStackedCharcoals.add(new ModDataComponents.CharcoalData(cd.size(), cd.remainingTicks(), newProgress));
                        } else {
                            anyChanged = true;
                        }
                    }

                    if (anyChanged) {
                        calloudStack.set(ModDataComponents.CALLOUD_CONTENTS.get(), new ModDataComponents.CalloudData(newCharcoals, newStackedCharcoals));
                        needsSync = true;
                    }
                }
            }
        }

        if (isPuffing) {
            boolean holdingPipe = player.getMainHandItem().getItem() == ModItems.HOOKAH_PIPE.get() ||
                    player.getOffhandItem().getItem() == ModItems.HOOKAH_PIPE.get();
            if (!holdingPipe) {
                player.sendSystemMessage(Component.literal("You let go of the hookah pipe!"));
                stopPuffing();
                return;
            }
            if (!tryPuff()) {
                stopPuffing();
                return;
            }
        }

        if (needsSync) {
            syncToClient();
            needsSync = false;
        }
    }

    public void syncToClient() {
        PacketDistributor.sendToPlayer(player, new PacketBackpackHookahPuffData(
                getTotalHeat(), totalTobaccoWeight, cachedBowlContents));
    }

    public void saveToPlayer() {
        HolderLookup.Provider registries = player.registryAccess();
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = new CompoundTag();

        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("TotalTobaccoWeight", totalTobaccoWeight);
        tag.putInt("LastGramsCount", lastGramsCount);
        tag.putBoolean("IsPuffing", isPuffing);
        tag.putInt("PuffDurationTicks", puffDurationTicks);
        tag.putInt("AirConsumed", airConsumed);

        root.put(NBT_KEY, tag);
    }

    public void loadFromPlayer() {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_KEY)) return;

        CompoundTag tag = root.getCompound(NBT_KEY);
        HolderLookup.Provider registries = player.registryAccess();

        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));

        ItemStack slot1Item = itemHandler.getStackInSlot(1);
        if (!slot1Item.isEmpty() && slot1Item.is(ModItems.HOOKAH_BOWL.get())) {
            readBowlContents(slot1Item);
        } else if (!slot1Item.isEmpty()) {
            totalTobaccoWeight = tag.getInt("TotalTobaccoWeight");
            lastGramsCount = totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM;
            itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        }

        lastGramsCount = tag.contains("LastGramsCount") ? tag.getInt("LastGramsCount") : lastGramsCount;
        // Никогда не восстанавливаем активную затяжку: трубка после релога не зажата,
        // иначе игрок «затягивается» без трубки и теряет воздух.
        isPuffing = false;
        puffDurationTicks = tag.getInt("PuffDurationTicks");
        airConsumed = tag.getInt("AirConsumed");
    }

    public int getTotalTobaccoWeight() { return totalTobaccoWeight; }
    @Nullable
    public ModDataComponents.HookahBowlContents getCachedBowlContents() { return cachedBowlContents; }

    public void drops() {
        net.minecraft.world.SimpleContainer inventory = new net.minecraft.world.SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        net.minecraft.world.Containers.dropContents(player.level(), player.blockPosition(), inventory);
    }

    public void resetState() {
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        totalTobaccoWeight = 0;
        tobaccoOverBurnAccumulator = 0f;
        cachedBowlContents = null;
        lastGramsCount = 0;
        isPuffing = false;
        puffDurationTicks = 0;
        airConsumed = 0;
        player.getPersistentData().remove(NBT_KEY);
    }

    // === Static state management ===

    private static final Map<UUID, BackpackHookahState> STATES = new HashMap<>();

    public static BackpackHookahState getOrCreate(ServerPlayer player) {
        BackpackHookahState state = STATES.get(player.getUUID());
        if (state == null) {
            state = new BackpackHookahState(player);
            STATES.put(player.getUUID(), state);
            state.loadFromPlayer();
        } else if (state.player != player) {
            // Игрок пересоздан (респаун/смена измерения) — перепривязываем к живому объекту,
            // иначе тик/сохранение уходят в удалённую сущность.
            state.player = player;
        }
        return state;
    }

    @Nullable
    public static BackpackHookahState getState(UUID playerUUID) {
        return STATES.get(playerUUID);
    }

    public static void removeState(UUID playerUUID) {
        STATES.remove(playerUUID);
    }

    // === Effect helpers ===

    private Holder<MobEffect> getMobEffectForFlavor(String flavor) {
        return switch (flavor) {
            case "mint" -> MobEffects.MOVEMENT_SPEED;
            case "citrus" -> MobEffects.DAMAGE_BOOST;
            case "berry" -> MobEffects.JUMP;
            default -> null;
        };
    }

    private void applyTobaccoEffects(ServerPlayer player, int totalPuffDurationTicks, ModDataComponents.HookahBowlContents contents) {
        boolean compatible = contents.isCompatible();

        if (!compatible) {
            player.sendSystemMessage(Component.translatable("quasium.message.incompatible_mix_effects"));
            applyNauseaFromNegativeKaif();
            return;
        }

        Map<Integer, Integer> groupPercentages = contents.getGroupPercentages();

        for (int groupIdx = 0; groupIdx < contents.groups().size(); groupIdx++) {
            ModDataComponents.BowlTobaccoGroup group = contents.groups().get(groupIdx);
            Integer pctObj = groupPercentages.get(groupIdx);
            if (pctObj == null) continue;
            int percentage = pctObj;
            int durationSeconds = (int) Math.ceil(percentage * totalPuffDurationTicks / (double) TICKS_PER_SECOND / 100.0);
            int durationTicks = durationSeconds * TICKS_PER_SECOND;
            if (durationTicks <= 0) continue;

            if (group.isRegular()) {
                Holder<MobEffect> effect = getMobEffectForFlavor(group.flavor());
                if (effect != null) {
                    int amplifier = percentage > 70 ? 1 : 0;
                    player.addEffect(new MobEffectInstance(effect, durationTicks, amplifier));
                }
            } else {
                float strength = group.strength();
                for (MobEffectInstance templateEffect : group.effects()) {
                    int amplifier = templateEffect.getAmplifier();
                    if (strength > 0.5f) amplifier += 1;
                    player.addEffect(new MobEffectInstance(templateEffect.getEffect(), durationTicks, amplifier));
                }
            }
        }
    }

    private void applyNauseaFromNegativeKaif() {
        int kaif = PlayerKaifData.getKaifValue(player);
        if (kaif < 0) {
            int nauseaLevel = Math.abs(kaif) / 10;
            if (nauseaLevel > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, TICKS_PER_SECOND * 5, Math.min(nauseaLevel - 1, 4)));
            }
        }
    }
}
