package net.sorukoma.quasium.data;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.PureTobaccoSmokedTrigger;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerOverdoseData;
import net.sorukoma.quasium.item.CalloudItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.entity.ToxicSmokeEntity;
import net.sorukoma.quasium.hookah.BowlLayers;
import net.sorukoma.quasium.hookah.ExhaleSmoke;
import net.sorukoma.quasium.hookah.HookahSignals;
import net.sorukoma.quasium.hookah.PuffEffects;
import net.sorukoma.quasium.magic.KaifZones;
import net.sorukoma.quasium.selection.variety.VarietySmoke;
import net.sorukoma.quasium.magic.SmokeCasting;
import net.sorukoma.quasium.magic.SmokeSource;
import net.sorukoma.quasium.mind.MindStats;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.network.PacketBackpackHookahPuffData;
import net.sorukoma.quasium.network.PacketSyncBackpackPipeConnection;
import net.sorukoma.quasium.util.HookahLogic;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BackpackHookahState implements SmokeSource {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int TICKS_PER_SECOND = 20;
    public static final int TOBACCO_TICKS_PER_GRAM = 250;
    public static final int MAX_TOBACCO_WEIGHT = 20 * TOBACCO_TICKS_PER_GRAM;
    public static final int AIR_CONSUMPTION_PER_TICK_PUFF = 6;
    public static final int CALLOUD_IDLE_COOLING_FACTOR = 5;

    private static final String NBT_KEY = "QuasiumBackpackHookah";

    private int totalTobaccoWeight = 0;
    @Nullable
    private ModDataComponents.HookahBowlContents cachedBowlContents = null;
    private int lastGramsCount = 0;
    private float burnAccumulator = 0f;
    private int warmupPuffsRemaining = HookahLogic.WARMUP_PUFFS;
    @Nullable
    private Boolean wasReady;
    private boolean overheatWarned;
    private int idleTicks = HookahLogic.COALS_HOLD_HEAT_TICKS;

    private boolean isPuffing = false;
    private int puffDurationTicks = 0;
    private int airConsumed = 0;
    private int kaifAccumulator = 0;
    private float puffEfficiencyAccumulator = 0f;

    private boolean needsSync = false;
    private final BowlLayers layers = new BowlLayers();

    // Не final: на респауне/смене измерения создаётся новый ServerPlayer,
    // ссылку нужно обновлять, иначе тик пишет в мёртвый объект.
    private ServerPlayer player;

    private final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot == 1) {
                if (stack.isEmpty()) {
                    super.setStackInSlot(slot, ItemStack.EMPTY);
                    totalTobaccoWeight = 0;
                    burnAccumulator = 0f;
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
            return slot == 0 ? super.getSlotLimit(slot) : 1;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return slot == 0 ? super.getStackLimit(slot, stack) : 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (player != null && !player.level().isClientSide()) {
                needsSync = true;
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof CalloudItem;
            if (slot == 1) return stack.is(ModItems.HOOKAH_BOWL.get());
            if (slot == 2) return stack.is(ModItems.HOOKAH_DIFFUSER.get());
            if (slot == 3) return stack.is(ModItems.HOOKAH_COOLER.get());
            if (slot == 4) return stack.is(ModItems.HOOKAH_ICE_CHAMBER.get());
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
                    burnAccumulator = 0f;
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

    public void setPlayer(ServerPlayer player) {
        this.player = player;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public boolean hasDiffuser() { return !itemHandler.getStackInSlot(2).isEmpty(); }
    public boolean hasCooler() { return !itemHandler.getStackInSlot(3).isEmpty(); }
    public boolean hasIceChamber() { return !itemHandler.getStackInSlot(4).isEmpty(); }

    public boolean isHookahContent(ItemStack stack) {
        return itemHandler.isItemValid(1, stack);
    }

    @Override
    public ItemStack smokeBowl() {
        return itemHandler.getStackInSlot(1);
    }

    @Override
    @Nullable
    public ModDataComponents.HookahBowlContents burningRow() {
        return layers.burning(totalTobaccoWeight);
    }

    @Override
    public boolean atPeak() {
        return HookahLogic.getSmokingStage(warmupPuffsRemaining, totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM) == HookahLogic.SmokingStage.PEAK;
    }

    @Override
    public boolean burnForCast(ServerPlayer caster, int units) {
        if (totalTobaccoWeight < units) return false;
        if (units <= 0) return true;
        consume(units, caster);
        return true;
    }

    private void consume(int units, ServerPlayer smoker) {
        int gramsBefore = totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM;
        ModDataComponents.HookahBowlContents burning = burningRow();
        totalTobaccoWeight = Math.max(0, totalTobaccoWeight - units);
        PlayerOverdoseData.recordConsumption(smoker, units, burning);
        afterTobaccoConsumed();
        layers.burned(smoker, top(), totalTobaccoWeight);
        if (warmupPuffsRemaining == 0 && totalTobaccoWeight > 0 && gramsBefore > HookahLogic.TAIL_GRAMS
                && totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM <= HookahLogic.TAIL_GRAMS) HookahSignals.tail(smoker);
    }

    private Vec3 top() {
        return player.position().add(0, 1.2, 0);
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

    public float heatThreshold() {
        return HookahLogic.heatThreshold(cachedBowlContents);
    }

    public int warmupPuffsRemaining() {
        return warmupPuffsRemaining;
    }

    private void readBowlContents(ItemStack bowlItem) {
        ModDataComponents.HookahBowlContents contents = bowlItem.getOrDefault(
                ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                ModDataComponents.HookahBowlContents.cleared());
        if (!contents.slotAssignments().isEmpty() || !contents.burnedSlots().isEmpty()) {
            if (!contents.heated()) warmupPuffsRemaining = HookahLogic.WARMUP_PUFFS;
            contents = contents.withHeated(true);
            bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), contents);
            totalTobaccoWeight = contents.filledSlotCount() * TOBACCO_TICKS_PER_GRAM;
            cachedBowlContents = contents;
            lastGramsCount = contents.filledSlotCount();
        } else {
            totalTobaccoWeight = 0;
            burnAccumulator = 0f;
            cachedBowlContents = null;
            lastGramsCount = 0;
        }
        layers.reset(cachedBowlContents);
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
        if (player != null) SmokeCasting.pullStarted(player);
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
        boolean counted = SmokeCasting.endPull(player);

        // Compute effective duration from average puff efficiency
        int effectivePuffDuration = 0;
        if (puffDurationTicks > 0) {
            float avgPuffEfficiency = puffEfficiencyAccumulator / puffDurationTicks;
            effectivePuffDuration = (int)(puffDurationTicks * avgPuffEfficiency);
        }

        if (counted) MindStats.puff(player);
        if (counted && effectivePuffDuration > 0 && cachedBowlContents != null) {
            PuffEffects.apply(player, effectivePuffDuration, cachedBowlContents);
            for (ModDataComponents.BowlTobaccoGroup group : cachedBowlContents.groups()) {
                if (group.isRegular()) {
                    PlayerTobaccoData.addTriedFlavors(player, Set.of(group.flavor()));
                    PlayerTobaccoData.addFirstFlavorResult(player, group.flavor());
                } else {
                    PlayerTobaccoData.addTriedFlavors(player, Set.of("self_mixed_" + group.base()));
                    PlayerTobaccoData.addFirstFlavorResult(player, group.flavor());
                }
            }
            PureTobaccoSmokedTrigger.fire(player, cachedBowlContents);
            layers.exhaled();
            if (warmupPuffsRemaining > 0 && --warmupPuffsRemaining == 0 && atPeak()) HookahSignals.peak(player, top());
        }

        if (counted) exhale(effectivePuffDuration);
        needsSync = true;
    }

    private void exhale(int effectivePuffDuration) {
        ExhaleSmoke.exhale(player, burningRow());
        ItemStack bowl = itemHandler.getStackInSlot(1);
        String essences = bowl.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "");
        boolean freezing = (essences.contains("mint") && essences.contains("ice"))
                || (hasIceChamber() && player.level().random.nextFloat() < 0.25f);
        // Токсичность берём из групп содержимого (мутация "toxic" самозамеса), а не из компонента чаши.
        boolean toxic = cachedBowlContents != null && cachedBowlContents.groups().stream()
                .anyMatch(ModDataComponents.BowlTobaccoGroup::toxic);
        if (toxic || freezing) {
            float strength = cachedBowlContents == null ? 0f : (float)cachedBowlContents.groups().stream()
                    .mapToDouble(ModDataComponents.BowlTobaccoGroup::strength).average().orElse(0d);
            net.minecraft.world.phys.Vec3 look = player.getLookAngle();
            ToxicSmokeEntity smoke = new ToxicSmokeEntity(player.level(), player,
                    player.getEyePosition().add(look.scale(0.35)), look.scale(0.45),
                    toxic ? Math.max(1f, strength * 4f) : 0f, freezing);
            player.level().addFreshEntity(smoke);
        }

        if (effectivePuffDuration > 0) VarietySmoke.onExhale(player, burningRow());
    }

    public boolean isPuffing() {
        return isPuffing;
    }

    @Nullable
    public String coldReason() {
        if (!(itemHandler.getStackInSlot(0).getItem() instanceof CalloudItem)) return "needs_calloud";
        if (totalTobaccoWeight <= 0) return "needs_content";
        return HookahLogic.lit(getTotalHeat(), heatThreshold(), hasCooler()) ? null : "not_enough_heat";
    }

    private boolean unlit(String reason) {
        stopPuffing();
        HookahSignals.unlit(player, reason);
        return false;
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

        String cold = coldReason();
        if (cold != null) return unlit(cold);

        ItemStack calloudStack = itemHandler.getStackInSlot(0);
        ModDataComponents.CalloudData calloudData = calloudStack.getOrDefault(
                ModDataComponents.CALLOUD_CONTENTS.get(),
                new ModDataComponents.CalloudData(List.of(), List.of())
        );

        float totalHeat = calloudData.totalHeat();
        float physicalHeatThreshold = heatThreshold();
        idleTicks = 0;

        // Puff efficiency: if heatRatio < 60% of max possible heat, effects scale down
        float maxPossibleHeat = calloudData.maxPossibleHeat();
        float heatRatio = maxPossibleHeat > 0f ? totalHeat / maxPossibleHeat : 0f;
        HookahLogic.SmokingStage smokingStage = HookahLogic.getSmokingStage(warmupPuffsRemaining,
                totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM);
        final float puffEfficiency = HookahLogic.getPuffEfficiency(heatRatio, smokingStage, hasDiffuser());

        SmokeCasting.PuffResult castResult = SmokeCasting.onPuffTick(player, this,
                HookahLogic.getPuffEfficiency(heatRatio, HookahLogic.SmokingStage.PEAK, hasDiffuser()));
        if (castResult == SmokeCasting.PuffResult.COUGH) return false;
        if (castResult == SmokeCasting.PuffResult.HOLD) return true;

        int coalBurn = ((CalloudItem) calloudStack.getItem()).burnsForever() || hasDiffuser() && player.level().getGameTime() % 2L == 0L ? 0 : 1;

        // Decay each charcoal's remaining ticks (1 tick of puff = 1 tick of burn)
        List<ModDataComponents.CharcoalData> newCharcoals = new ArrayList<>();
        boolean anyRemoved = false;
        for (ModDataComponents.CharcoalData cd : calloudData.charcoals()) {
            int newRemaining = Math.max(0, cd.remainingTicks() - coalBurn);
            if (newRemaining > 0) {
                newCharcoals.add(new ModDataComponents.CharcoalData(cd.size(), newRemaining, cd.heatingProgress()));
            } else {
                anyRemoved = true;
            }
        }

        // Decay stacked charcoals remaining ticks too
        List<ModDataComponents.CharcoalData> newStackedCharcoals = new ArrayList<>();
        for (ModDataComponents.CharcoalData cd : calloudData.stackedCharcoals()) {
            int newRemaining = Math.max(0, cd.remainingTicks() - coalBurn);
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
            boolean overheat = totalHeat > HookahLogic.overheatLine(physicalHeatThreshold);
            if (overheat && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && player.tickCount % 10 == 0) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        player.getX(), player.getY() + 1.3, player.getZ(), 4, 0.2, 0.15, 0.2, 0.02);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        player.getX(), player.getY() + 1.4, player.getZ(), 2, 0.15, 0.1, 0.15, 0.01);
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.2f, 0.7f);
            }
            if (overheat && !overheatWarned) HookahSignals.overheat(player);
            overheatWarned = overheat;
            int burnedSlots = cachedBowlContents != null ? cachedBowlContents.burnedSlots().size() : 0;
            burnAccumulator += HookahLogic.burnPerTick(totalHeat, physicalHeatThreshold,
                    totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM + burnedSlots);
            int consumption = (int) burnAccumulator;
            burnAccumulator -= consumption;
            if (consumption > 0) consume(consumption, player);
        }

        puffDurationTicks++;
        airConsumed += AIR_CONSUMPTION_PER_TICK_PUFF;
        puffEfficiencyAccumulator += puffEfficiency;
        player.setAirSupply(Math.max(0, player.getAirSupply() - Perk.air(player, AIR_CONSUMPTION_PER_TICK_PUFF)));

        // Бульканье колбы: короткий звук каждые ~20 тиков, pitch зависит от эффективности затяжки
        if (puffDurationTicks % 20 == 0) {
            float bubblePitch = 0.8f + puffEfficiency * 0.6f;
            player.level().playSound(null, player.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 0.35f, bubblePitch);
        }

        // Kaif — check compatibility
        boolean isCompatible = cachedBowlContents != null && cachedBowlContents.isCompatible();

        kaifAccumulator++;
        if (kaifAccumulator >= TICKS_PER_SECOND) {
            int kaifPerSecond = Config.KAIF_PER_SECOND_SMOKING.getAsInt();
            if (kaifPerSecond > 0) {
                float strength = HookahLogic.strength(cachedBowlContents, 0.4d, 0.3d);
                int adjustedKaif = HookahLogic.calculateKaif(kaifPerSecond, puffEfficiency, strength);
                if (isCompatible) {
                    String essences = itemHandler.getStackInSlot(1).getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "");
                    if (smokingStage == HookahLogic.SmokingStage.TAIL && player.level().random.nextFloat() < 0.25f) {
                        PlayerKaifData.subtractKaif(player, 1);
                    } else {
                        int flavorBaseKaif = PuffEffects.containsCitrus(essences) && essences.contains("mint")
                                ? Math.max(1, adjustedKaif / 2) : Math.max(1, adjustedKaif);
                        int flavorAdjustedKaif = PlayerFlavorPreferenceData.adjustKaif(
                                player, burningRow(), flavorBaseKaif, !layers.fresh());
                        if (flavorAdjustedKaif > 0) {
                            PlayerKaifData.addKaif(player, flavorAdjustedKaif);
                        } else if (flavorAdjustedKaif < 0) {
                            PlayerKaifData.subtractKaif(player, -flavorAdjustedKaif);
                        }
                    }
                } else {
                    PlayerKaifData.subtractKaif(player, kaifPerSecond);
                    PuffEffects.nauseaFromNegativeKaif(player);
                }
            }
            kaifAccumulator = 0;
        }

        return true;
    }

    private void afterTobaccoConsumed() {
        if (totalTobaccoWeight == 0) {
            lastGramsCount = 0;

            ItemStack bowlItem = itemHandler.getStackInSlot(1);
            if (!bowlItem.isEmpty() && bowlItem.is(ModItems.HOOKAH_BOWL.get())) {
                ModDataComponents.HookahBowlContents remainingContents = bowlItem.getOrDefault(
                        ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                        new ModDataComponents.HookahBowlContents(List.of(), Map.of(), Set.of(), false));
                while (!remainingContents.slotAssignments().isEmpty()) {
                    remainingContents = remainingContents.burnLastSlot();
                }
                bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), remainingContents);
                cachedBowlContents = remainingContents.burnedSlots().isEmpty() ? null : remainingContents;
            } else {
                cachedBowlContents = null;
            }
        } else {
            updateBowlItemDuringConsumption();
        }
        needsSync = true;
    }

    public void tick() {
        // Idle coal decay: when not puffing, coals cool down slowly (remainingTicks stays — consistent with heater)
        if (!isPuffing && idleTicks < HookahLogic.COALS_HOLD_HEAT_TICKS) {
            idleTicks++;
        } else if (!isPuffing) {
            ItemStack calloudStack = itemHandler.getStackInSlot(0);
            if (calloudStack.getItem() instanceof CalloudItem calloud && !calloud.burnsForever()) {
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
                            float coolingPerTick = 100.0f / (cd.size().getMaxTicks() * CALLOUD_IDLE_COOLING_FACTOR) * Perk.coolingMultiplier(player);
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
                            float coolingPerTick = 100.0f / (cd.size().getMaxTicks() * CALLOUD_IDLE_COOLING_FACTOR) * Perk.coolingMultiplier(player);
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
                        needsSync |= player != null && player.tickCount % 20 == 0;
                    }
                }
            }
        }

        if (player != null && player.tickCount % KaifZones.TICK_INTERVAL == 0) {
            boolean ready = mood() != HookahLogic.Mood.COLD;
            if (ready && Boolean.FALSE.equals(wasReady)) HookahSignals.ready(player.serverLevel(), top());
            wasReady = ready;
        }

        if (isPuffing) {
            boolean holdingPipe = player.getMainHandItem().getItem() == ModItems.HOOKAH_PIPE.get() ||
                    player.getOffhandItem().getItem() == ModItems.HOOKAH_PIPE.get();
            if (!holdingPipe) {
                player.displayClientMessage(Component.translatable("quasium.message.let_go"), true);
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
                getTotalHeat(), totalTobaccoWeight, warmupPuffsRemaining, cachedBowlContents));
    }

    public HookahLogic.Mood mood() {
        return HookahLogic.mood(getTotalHeat(), heatThreshold(), hasCooler(), warmupPuffsRemaining, totalTobaccoWeight);
    }

    public void saveToPlayer() {
        player.getPersistentData().put(NBT_KEY, save(player.registryAccess()));
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();

        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("TotalTobaccoWeight", totalTobaccoWeight);
        tag.putInt("LastGramsCount", lastGramsCount);
        tag.putBoolean("IsPuffing", isPuffing);
        tag.putInt("PuffDurationTicks", puffDurationTicks);
        tag.putInt("AirConsumed", airConsumed);
        tag.putInt("WarmupPuffsRemaining", warmupPuffsRemaining);

        return tag;
    }

    public void loadFromPlayer() {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_KEY)) return;
        load(root.getCompound(NBT_KEY), player.registryAccess());
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));

        ItemStack slot1Item = itemHandler.getStackInSlot(1);
        if (!slot1Item.isEmpty() && slot1Item.is(ModItems.HOOKAH_BOWL.get())) {
            readBowlContents(slot1Item);
            if (tag.contains("TotalTobaccoWeight")) {
                totalTobaccoWeight = Math.max(0, Math.min(tag.getInt("TotalTobaccoWeight"),
                        cachedBowlContents == null ? 0 : cachedBowlContents.filledSlotCount() * TOBACCO_TICKS_PER_GRAM));
            }
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
        warmupPuffsRemaining = tag.contains("WarmupPuffsRemaining") ? tag.getInt("WarmupPuffsRemaining") : HookahLogic.WARMUP_PUFFS;
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
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        totalTobaccoWeight = 0;
        burnAccumulator = 0f;
        cachedBowlContents = null;
        lastGramsCount = 0;
        isPuffing = false;
        puffDurationTicks = 0;
        airConsumed = 0;
        warmupPuffsRemaining = HookahLogic.WARMUP_PUFFS;
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

    public static void takenOff(ServerPlayer player) {
        BackpackHookahState state = STATES.remove(player.getUUID());
        if (state == null) return;
        state.stopPuffing();
        state.drops();
        state.resetState();
        PacketDistributor.sendToPlayer(player, new PacketSyncBackpackPipeConnection(false));
    }
}
