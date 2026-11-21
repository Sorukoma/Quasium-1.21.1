package net.sorukoma.quasium.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.PureTobaccoSmokedTrigger;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerTobaccoData;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.entity.ToxicSmokeEntity;
import net.sorukoma.quasium.hookah.BowlLayers;
import net.sorukoma.quasium.hookah.ExhaleSmoke;
import net.sorukoma.quasium.hookah.HookahSignals;
import net.sorukoma.quasium.hookah.PuffEffects;
import net.sorukoma.quasium.selection.variety.VarietySmoke;
import net.sorukoma.quasium.item.CalloudItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.SmokeCasting;
import net.sorukoma.quasium.magic.SmokeSource;
import net.sorukoma.quasium.mind.MindStats;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.network.PacketHookahPuffData;
import net.sorukoma.quasium.util.SmokeColorUtil;
import net.sorukoma.quasium.util.HookahLogic;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.sorukoma.quasium.screen.HookahMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Block entity для кальяна — bowl-based логика с поддержкой обычных табаков и самозамесов.
 *
 * Slot 0: CalloudItem (каллауд с углями)
 * Slot 1: HookahBowlItem (max 1)
 *
 * Все данные о табаке хранятся в HookahBowlContents DataComponent
 * на ItemStack чаши (group-based модель: BowlTobaccoGroup + slotAssignments).
 *
 * Механика жара: кальян курится только при наличии каллауда с достаточной суммарной силой жара.
 * Уголь постепенно теряет жар. Если жар падает ниже порога — курение прекращается.
 */
public class HookahBlockEntity extends BlockEntity implements MenuProvider, SmokeSource {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ─── Constants ───
    public static final int TICKS_PER_SECOND = 20;
    public static final int TOBACCO_TICKS_PER_GRAM = 250;
    public static final int MAX_TOBACCO_WEIGHT = 20 * TOBACCO_TICKS_PER_GRAM;
    public static final int AIR_CONSUMPTION_PER_TICK_PUFF = 6;
    public static final int MAX_PUFFS_PER_POTION = 60 * TICKS_PER_SECOND;
    private static final int NAUSEA_PER_10_NEGATIVE_KAIF = 1;
    public static final int CALLOUD_IDLE_COOLING_FACTOR = 5;

    private int totalTobaccoWeight = 0;
    private float burnAccumulator = 0f;
    @Nullable
    private ModDataComponents.HookahBowlContents cachedBowlContents = null;
    private int lastGramsCount = 0;
    private int warmupPuffsRemaining = HookahLogic.WARMUP_PUFFS;
    private int flaskCharges;
    @Nullable
    private Boolean wasReady;
    private boolean overheatWarned;
    private long golemPuffedAt = Long.MIN_VALUE;

    // ─── Heat tracking (для синхронизации с клиентом) ───
    private boolean needsSync = false;
    private float lastSyncedTotalHeat = -1f;
    private int lastSyncedCharcoalCount = -1;
    private int lastSyncedTotalTobaccoWeight = Integer.MIN_VALUE;
    @Nullable
    private ModDataComponents.HookahBowlContents lastSyncedBowlContents = null;

    // ─── Puff tracking per player ───
    private final Set<HookahConnectionEntity> activeConnections = new HashSet<>();
    private UUID lastSmoker;
    private final Map<UUID, Integer> playerPuffDurations = new HashMap<>();
    private final Map<UUID, Integer> playerAirConsumed = new HashMap<>();
    private final Map<UUID, Integer> playerKaifAccumulators = new HashMap<>();
    private final Map<UUID, Float> playerPuffEfficiencyAccumulators = new HashMap<>();
    private final net.sorukoma.quasium.magic.HookahZone zone = new net.sorukoma.quasium.magic.HookahZone();
    private final BowlLayers layers = new BowlLayers();

    // ─── Item handler (Slot 0: Calloud, Slot 1: Bowl, Slot 2: Diffuser, Slot 3: Cooler, Slot 4: Ice Chamber) ───
    private final ItemStackHandler itemHandler = new ItemStackHandler(5) {
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
        public int getSlotLimit(int slot) {
            return slot == 0 ? super.getSlotLimit(slot) : 1;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return slot == 0 ? super.getStackLimit(slot, stack) : 1;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot == 1) {
                if (stack.isEmpty()) {
                    super.setStackInSlot(slot, ItemStack.EMPTY);
                    clearBowlState();
                    needsSync = true;
                } else if (stack.is(ModItems.HOOKAH_BOWL.get())) {
                    super.setStackInSlot(slot, stack.copyWithCount(1));
                    readBowlContents(itemHandler.getStackInSlot(1));
                    needsSync = true;
                }
                return;
            }
            super.setStackInSlot(slot, stack);
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
                    super.setStackInSlot(slot, ItemStack.EMPTY);
                    clearBowlState();
                    needsSync = true;
                    return current.copy();
                }
                return current.copy();
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide()) {
                setChanged();
                needsSync = true;
            }
        }
    };

    // ─── Constructor ───
    public HookahBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.HOOKAH_BE.get(), pPos, pBlockState);
    }

    /** Для подклассов (модульный кальян) с собственным типом BlockEntity. */
    protected HookahBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pPos, BlockState pBlockState) {
        super(type, pPos, pBlockState);
    }

    /**
     * Множитель качества затяжки от 0 до 1, применяемый к эффективности затяжки.
     * База — 1.0 (без влияния). Подклассы (модульный кальян) переопределяют,
     * чтобы уровень воды в колбе снижал качество покура.
     */
    protected float getPuffQualityMultiplier() {
        return 1.0f;
    }

    // ─── Public accessors ───
    public ItemStackHandler getItemHandler() { return itemHandler; }
    public int getTotalTobaccoWeight() { return totalTobaccoWeight; }
    @Nullable
    public ModDataComponents.HookahBowlContents getCachedBowlContents() { return cachedBowlContents; }
    public int getMaxPuffsPerPotion() { return MAX_PUFFS_PER_POTION; }
    public boolean isPlayerPuffing(UUID playerUUID) { return playerPuffDurations.containsKey(playerUUID); }
    public boolean hasDiffuser() { return !itemHandler.getStackInSlot(2).isEmpty(); }
    public boolean hasCooler() { return !itemHandler.getStackInSlot(3).isEmpty(); }
    public boolean hasIceChamber() { return !itemHandler.getStackInSlot(4).isEmpty(); }

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

    public int flaskCharges() {
        return flaskCharges;
    }

    public boolean addFlaskCharge() {
        if (flaskCharges >= Config.FLASK_MAX_CHARGES.getAsInt()) return false;
        flaskCharges++;
        setChanged();
        return true;
    }

    @Override
    public boolean useFlaskCharge() {
        if (flaskCharges <= 0) return false;
        flaskCharges--;
        setChanged();
        return true;
    }

    public int warmupPuffsRemaining() {
        return warmupPuffsRemaining;
    }

    public float heatThreshold() {
        return HookahLogic.heatThreshold(cachedBowlContents);
    }

    public HookahLogic.Mood mood() {
        return HookahLogic.mood(getTotalHeat(), heatThreshold(), hasCooler(), warmupPuffsRemaining, totalTobaccoWeight);
    }

    public boolean isSmoldering() {
        return mood() != HookahLogic.Mood.COLD;
    }

    public net.sorukoma.quasium.magic.HookahZone zone() {
        return zone;
    }

    public boolean canBottleSmoke() {
        return level != null && isSmoldering() && atPeak() && zone.puffedWithin(level.getGameTime(), 100);
    }

    public String zoneFlavor() {
        return BowlLayers.flavor(burningRow());
    }

    @Override
    public boolean burnForCast(ServerPlayer player, int units) {
        if (totalTobaccoWeight < units) return false;
        if (units <= 0) return true;
        consume(units, player);
        setChanged();
        return true;
    }

    private void consume(int units, @Nullable Player player) {
        int gramsBefore = totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM;
        ModDataComponents.HookahBowlContents burning = burningRow();
        totalTobaccoWeight = Math.max(0, totalTobaccoWeight - units);
        if (player instanceof ServerPlayer smoker) {
            net.sorukoma.quasium.data.PlayerOverdoseData.recordConsumption(smoker, units, burning);
        }
        afterTobaccoConsumed(itemHandler.getStackInSlot(1));
        if (player instanceof ServerPlayer smoker) layers.burned(smoker, top(), totalTobaccoWeight);
        boolean tail = warmupPuffsRemaining == 0 && totalTobaccoWeight > 0 && gramsBefore > HookahLogic.TAIL_GRAMS
                && totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM <= HookahLogic.TAIL_GRAMS;
        Player told = player != null || lastSmoker == null ? player : level.getPlayerByUUID(lastSmoker);
        if (tail && told instanceof ServerPlayer smoker) HookahSignals.tail(smoker);
    }

    private Vec3 top() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, 1.05, 0);
    }

    @Nullable
    public String coldReason() {
        if (!(itemHandler.getStackInSlot(0).getItem() instanceof CalloudItem)) return "needs_calloud";
        ItemStack bowl = itemHandler.getStackInSlot(1);
        if (!bowl.is(ModItems.HOOKAH_BOWL.get()) || totalTobaccoWeight <= 0) return "needs_content";
        return HookahLogic.lit(getTotalHeat(), heatThreshold(), hasCooler()) ? null : "not_enough_heat";
    }

    private boolean unlit(Player player, String reason) {
        stopPlayerConnection(player);
        if (player instanceof ServerPlayer smoker) HookahSignals.unlit(smoker, reason);
        return false;
    }

    private void stopPlayerConnection(Player player) {
        for (HookahConnectionEntity connection : new ArrayList<>(activeConnections)) {
            if (player.getUUID().equals(connection.getAttachedPlayerUUID())) {
                connection.stopPuffing(this, player);
                return;
            }
        }
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

    private void readBowlContents(ItemStack bowlItem) {
        ModDataComponents.HookahBowlContents contents = bowlItem.getOrDefault(
                ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                new ModDataComponents.HookahBowlContents(List.of(), Map.of(), Set.of(), false)
        );
        if (!contents.slotAssignments().isEmpty() || !contents.burnedSlots().isEmpty()) {
            if (!contents.heated()) warmupPuffsRemaining = HookahLogic.WARMUP_PUFFS;
            contents = contents.withHeated(true);
            bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), contents);
            totalTobaccoWeight = contents.filledSlotCount() * TOBACCO_TICKS_PER_GRAM;
            cachedBowlContents = contents;
            lastGramsCount = contents.filledSlotCount();
            warmupPuffsRemaining = Math.clamp(warmupPuffsRemaining, 0, HookahLogic.WARMUP_PUFFS);
        } else {
            totalTobaccoWeight = 0;
            burnAccumulator = 0f;
            cachedBowlContents = null;
            lastGramsCount = 0;
        }
        layers.reset(cachedBowlContents);
    }

    private void clearBowlState() {
        totalTobaccoWeight = 0;
        burnAccumulator = 0f;
        cachedBowlContents = null;
        lastGramsCount = 0;
    }

    private void updateBowlItemDuringConsumption() {
        ItemStack bowlItem = itemHandler.getStackInSlot(1);
        if (bowlItem.isEmpty() || !bowlItem.is(ModItems.HOOKAH_BOWL.get())) return;

        int currentGrams = totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM;
        if (currentGrams == lastGramsCount) return;

        int gramsToRemove = lastGramsCount - currentGrams;
        ModDataComponents.HookahBowlContents contents = bowlItem.getOrDefault(
                ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                new ModDataComponents.HookahBowlContents(List.of(), Map.of(), Set.of(), false)
        );

        for (int i = 0; i < gramsToRemove; i++) {
            contents = contents.burnLastSlot();
        }

        // If all slots are gone (both active and burned), clear the bowl completely
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

    // ─── Puff lifecycle ───

    public void startPuffing(HookahConnectionEntity connectionEntity) {
        activeConnections.add(connectionEntity);
        if (connectionEntity.getAttachedPlayerUUID() != null) {
            playerPuffDurations.put(connectionEntity.getAttachedPlayerUUID(), 0);
            lastSmoker = connectionEntity.getAttachedPlayerUUID();
            if (level != null && level.getPlayerByUUID(lastSmoker) instanceof ServerPlayer smoker) SmokeCasting.pullStarted(smoker);
        }
        needsSync = true;
    }

    public void stopPuffing(HookahConnectionEntity connectionEntity, Player player) {
        if (this.level == null || this.level.isClientSide()) return;

        UUID playerUUID = connectionEntity.getAttachedPlayerUUID();
        Integer totalPuffDurationTicks = playerUUID != null ? playerPuffDurations.remove(playerUUID) : null;
        Float puffEfficiencySum = playerUUID != null ? playerPuffEfficiencyAccumulators.remove(playerUUID) : null;
        if (playerUUID != null) {
            playerAirConsumed.remove(playerUUID);
            playerKaifAccumulators.remove(playerUUID);
        }

        // Compute average puff efficiency and effective duration for effects
        float avgPuffEfficiency = 1.0f;
        int effectivePuffDuration = totalPuffDurationTicks != null ? totalPuffDurationTicks : 0;
        if (puffEfficiencySum != null && totalPuffDurationTicks != null && totalPuffDurationTicks > 0) {
            avgPuffEfficiency = puffEfficiencySum / totalPuffDurationTicks;
            effectivePuffDuration = (int)(totalPuffDurationTicks * avgPuffEfficiency);
        }

        ServerPlayer serverPlayer = player instanceof ServerPlayer smoker ? smoker : null;
        boolean counted = serverPlayer != null && SmokeCasting.endPull(serverPlayer);
        if (counted) {
            MindStats.puff(serverPlayer);
            if (effectivePuffDuration > 0) {
                ItemStack bowlItem = itemHandler.getStackInSlot(1);
                ModDataComponents.HookahBowlContents effectContents = null;

                if (!bowlItem.isEmpty() && bowlItem.is(ModItems.HOOKAH_BOWL.get())) {
                    effectContents = bowlItem.getOrDefault(
                            ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                            new ModDataComponents.HookahBowlContents(List.of(), Map.of(), Set.of(), false)
                    );
                    if (effectContents.slotAssignments().isEmpty()) effectContents = null;
                }

                // Fallback to cached contents
                if (effectContents == null && cachedBowlContents != null) {
                    effectContents = cachedBowlContents;
                }

                if (effectContents != null) {
                    PuffEffects.apply(serverPlayer, effectivePuffDuration, effectContents);
                    // Track tried flavors/bases
                    for (ModDataComponents.BowlTobaccoGroup group : effectContents.groups()) {
                        if (group.isRegular()) {
                            PlayerTobaccoData.addTriedFlavors(serverPlayer, Set.of(group.flavor()));
                            PlayerTobaccoData.addFirstFlavorResult(serverPlayer, group.flavor());
                        } else {
                            PlayerTobaccoData.addTriedFlavors(serverPlayer, Set.of("self_mixed_" + group.base()));
                            PlayerTobaccoData.addFirstFlavorResult(serverPlayer, group.flavor());
                        }
                    }
                    PureTobaccoSmokedTrigger.fire(serverPlayer, effectContents);
                }
                layers.exhaled();
                if (warmupPuffsRemaining > 0 && --warmupPuffsRemaining == 0 && atPeak()) HookahSignals.peak(serverPlayer, top());
            }
        }

        if (totalTobaccoWeight <= 0) {
            clearBowlState();
        }

        connectionEntity.setPuffing(false);
        activeConnections.remove(connectionEntity);

        if (counted) exhale(serverPlayer, effectivePuffDuration);
        needsSync = true;
    }

    private void exhale(ServerPlayer smoker, int effectivePuffDuration) {
        ExhaleSmoke.exhale(smoker, burningRow());
        ItemStack bowl = itemHandler.getStackInSlot(1);
        String essences = bowl.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "");
        boolean freezing = (essences.contains("mint") && essences.contains("ice"))
                || (hasIceChamber() && level.random.nextFloat() < 0.25f);
        // Токсичность берём из групп содержимого (мутация "toxic" самозамеса), а не из компонента чаши.
        boolean toxic = cachedBowlContents != null && cachedBowlContents.groups().stream()
                .anyMatch(ModDataComponents.BowlTobaccoGroup::toxic);
        if (toxic || freezing) {
            float strength = cachedBowlContents == null ? 0f : (float)cachedBowlContents.groups().stream()
                    .mapToDouble(ModDataComponents.BowlTobaccoGroup::strength).average().orElse(Config.TOBACCO_STRENGTH_MIN.get());
            Vec3 look = smoker.getLookAngle();
            ToxicSmokeEntity smoke = new ToxicSmokeEntity(level, smoker,
                    smoker.getEyePosition().add(look.scale(0.35)), look.scale(0.45), toxic ? Math.max(1f, strength * 4f) : 0f, freezing);
            level.addFreshEntity(smoke);
        }
        if (effectivePuffDuration > 0) VarietySmoke.onExhale(smoker, burningRow());
    }

    public boolean tryPuff(Player player) {
        if (this.level == null || this.level.isClientSide()) return false;

        // Air check
        if (player.getAirSupply() <= AIR_CONSUMPTION_PER_TICK_PUFF) {
            List<HookahConnectionEntity> connectionsToRemove = new ArrayList<>();
            for (HookahConnectionEntity connection : activeConnections) {
                if (connection.getAttachedPlayerUUID() != null &&
                    connection.getAttachedPlayerUUID().equals(player.getUUID())) {
                    connectionsToRemove.add(connection);
                    ItemStack pipeItem = player.getMainHandItem().is(ModItems.HOOKAH_PIPE.get()) ?
                                         player.getMainHandItem() :
                                         player.getOffhandItem();
                    if (pipeItem.is(ModItems.HOOKAH_PIPE.get())) {
                        long cooldownEndTime = this.level.getGameTime() + 100L;
                        pipeItem.set(ModDataComponents.HOOKAH_PIPE_COOLDOWN_END.get(), cooldownEndTime);
                    }
                    if (connection.isPuffing()) {
                        connection.stopPuffing(this, player);
                    }
                    break;
                }
            }
            return false;
        }

        String cold = coldReason();
        if (cold != null) return unlit(player, cold);

        ItemStack calloudStack = itemHandler.getStackInSlot(0);
        ModDataComponents.CalloudData calloudData = calloudStack.getOrDefault(
                ModDataComponents.CALLOUD_CONTENTS.get(),
                new ModDataComponents.CalloudData(List.of(), List.of())
        );

        float totalHeat = calloudData.totalHeat();
        float physicalHeatThreshold = heatThreshold();

        zone.notePuff(player.getUUID(), level.getGameTime());
        lastSmoker = player.getUUID();

        // Puff efficiency: if heatRatio < 60% of max possible heat, effects scale down
        // heatRatio >= 0.6 → full efficiency (1.0). Below 0.6 → linear: at 60%=0.8, at 0%=0
        float maxPossibleHeat = calloudData.maxPossibleHeat();
        float heatRatio = maxPossibleHeat > 0f ? totalHeat / maxPossibleHeat : 0f;
        int remainingGrams = totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM;
        HookahLogic.SmokingStage smokingStage = HookahLogic.getSmokingStage(warmupPuffsRemaining, remainingGrams);
        final float puffEfficiency = HookahLogic.getPuffEfficiency(heatRatio, smokingStage, hasDiffuser())
                * getPuffQualityMultiplier();

        if (player instanceof ServerPlayer caster) {
            float heatEfficiency = HookahLogic.getPuffEfficiency(heatRatio, HookahLogic.SmokingStage.PEAK, hasDiffuser())
                    * getPuffQualityMultiplier();
            SmokeCasting.PuffResult castResult = SmokeCasting.onPuffTick(caster, this, heatEfficiency);
            if (castResult == SmokeCasting.PuffResult.COUGH) {
                stopPlayerConnection(player);
                return false;
            }
            if (castResult == SmokeCasting.PuffResult.HOLD) return true;
        }

        burnCoals(calloudStack, calloudData);
        burnTobacco(totalHeat, physicalHeatThreshold, player);

        // Player air consumption
        playerPuffDurations.compute(player.getUUID(), (uuid, duration) -> duration == null ? 1 : duration + 1);
        playerAirConsumed.compute(player.getUUID(), (uuid, consumed) -> consumed == null ? AIR_CONSUMPTION_PER_TICK_PUFF : consumed + AIR_CONSUMPTION_PER_TICK_PUFF);
        playerPuffEfficiencyAccumulators.compute(player.getUUID(), (uuid, acc) -> acc == null ? puffEfficiency : acc + puffEfficiency);
        player.setAirSupply(Math.max(0, player.getAirSupply() - Perk.air(player, AIR_CONSUMPTION_PER_TICK_PUFF)));

        // Бульканье колбы: короткий звук каждые ~20 тиков, pitch зависит от эффективности затяжки
        Integer durationSoFar = playerPuffDurations.get(player.getUUID());
        if (durationSoFar != null && durationSoFar % 20 == 0 && this.level != null && !this.level.isClientSide()) {
            float bubblePitch = 0.8f + puffEfficiency * 0.6f;
            this.level.playSound(null, this.worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                    hasDiffuser() ? 0.15f : 0.35f, bubblePitch);
        }

        // Kaif accumulation — check compatibility
        ModDataComponents.HookahBowlContents currentContents = cachedBowlContents;
        boolean isCompatible = currentContents != null && currentContents.isCompatible();

        playerKaifAccumulators.compute(player.getUUID(), (uuid, acc) -> acc == null ? 1 : acc + 1);
        if (playerKaifAccumulators.get(player.getUUID()) >= TICKS_PER_SECOND) {
            int kaifPerSecond = Config.KAIF_PER_SECOND_SMOKING.getAsInt();
            if (kaifPerSecond > 0 && player instanceof ServerPlayer) {
                float strength = HookahLogic.strength(currentContents, Config.TOBACCO_STRENGTH_MULTIPLIER.get(),
                        Config.TOBACCO_STRENGTH_BASE.get());
                int adjustedKaif = HookahLogic.calculateKaif(kaifPerSecond, puffEfficiency, strength);
                if (isCompatible) {
                    String essences = itemHandler.getStackInSlot(1).getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "");
                    if (smokingStage == HookahLogic.SmokingStage.TAIL && level.random.nextFloat() < 0.25f) {
                        PlayerKaifData.subtractKaif(player, 1);
                    } else {
                        int flavorBaseKaif = PuffEffects.containsCitrus(essences) && essences.contains("mint")
                                ? Math.max(1, adjustedKaif / 2) : Math.max(1, adjustedKaif);
                        int flavorAdjustedKaif = net.sorukoma.quasium.data.PlayerFlavorPreferenceData.adjustKaif(
                                player, burningRow(), flavorBaseKaif, !layers.fresh());
                        if (flavorAdjustedKaif > 0) {
                            PlayerKaifData.addKaif(player, flavorAdjustedKaif);
                        } else if (flavorAdjustedKaif < 0) {
                            PlayerKaifData.subtractKaif(player, -flavorAdjustedKaif);
                        }
                    }
                } else {
                    PlayerKaifData.subtractKaif(player, kaifPerSecond);
                    PuffEffects.nauseaFromNegativeKaif((ServerPlayer) player);
                }
            }
            playerKaifAccumulators.put(player.getUUID(), 0);
        }

        return true;
    }

    public boolean golemPuff(UUID golem) {
        ItemStack calloudStack = itemHandler.getStackInSlot(0);
        if (level == null || level.isClientSide() || !isSmoldering() || !(calloudStack.getItem() instanceof CalloudItem)) return false;
        ModDataComponents.CalloudData calloudData = calloudStack.getOrDefault(
                ModDataComponents.CALLOUD_CONTENTS.get(), new ModDataComponents.CalloudData(List.of(), List.of()));
        zone.noteGolemPuff(golem, level.getGameTime());
        golemPuffedAt = level.getGameTime();
        burnCoals(calloudStack, calloudData);
        burnTobacco(calloudData.totalHeat(), heatThreshold(), null);
        return true;
    }

    public void golemExhaled(@Nullable ServerPlayer owner) {
        if (level == null || warmupPuffsRemaining <= 0) return;
        warmupPuffsRemaining--;
        needsSync = true;
        setChanged();
        if (warmupPuffsRemaining > 0 || !atPeak()) return;
        if (owner != null) HookahSignals.peak(owner, top());
        else HookahSignals.peak((ServerLevel) level, top());
    }

    private void burnCoals(ItemStack calloudStack, ModDataComponents.CalloudData calloudData) {
        int coalBurn = ((CalloudItem) calloudStack.getItem()).burnsForever() || hasDiffuser() && level.getGameTime() % 2L == 0L ? 0 : 1;
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
    }

    private void burnTobacco(float totalHeat, float threshold, @Nullable Player player) {
        if (totalTobaccoWeight <= 0) return;
        boolean overheat = totalHeat > HookahLogic.overheatLine(threshold);
        if (overheat && level instanceof net.minecraft.server.level.ServerLevel serverLevel && level.getGameTime() % 10L == 0L) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    4, 0.2, 0.15, 0.2, 0.02);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                    2, 0.15, 0.1, 0.15, 0.01);
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2f, 0.7f);
        }
        if (player instanceof ServerPlayer smoker) {
            if (overheat && !overheatWarned) HookahSignals.overheat(smoker);
            overheatWarned = overheat;
        }
        int burnedSlots = cachedBowlContents != null ? cachedBowlContents.burnedSlots().size() : 0;
        burnAccumulator += HookahLogic.burnPerTick(totalHeat, threshold, totalTobaccoWeight / TOBACCO_TICKS_PER_GRAM + burnedSlots);
        int consumption = (int) burnAccumulator;
        burnAccumulator -= consumption;
        if (consumption > 0) consume(consumption, player);
    }

    private void afterTobaccoConsumed(ItemStack bowlItem) {
        if (totalTobaccoWeight == 0) {
            // Tobacco weight is 0 but burned slots may still exist
            if (!bowlItem.isEmpty() && bowlItem.is(ModItems.HOOKAH_BOWL.get())) {
                ModDataComponents.HookahBowlContents currentContents = bowlItem.getOrDefault(
                        ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                        new ModDataComponents.HookahBowlContents(List.of(), Map.of(), Set.of(), false)
                );
                if (!currentContents.slotAssignments().isEmpty() || !currentContents.burnedSlots().isEmpty()) {
                    // Burn any remaining active slots, keep burned slots
                    while (!currentContents.slotAssignments().isEmpty()) {
                        currentContents = currentContents.burnLastSlot();
                    }
                    if (currentContents.burnedSlots().isEmpty()) {
                        bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), currentContents.cleared());
                        cachedBowlContents = null;
                    } else {
                        bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), currentContents);
                        cachedBowlContents = currentContents;
                    }
                } else {
                    bowlItem.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), ModDataComponents.HookahBowlContents.cleared());
                    cachedBowlContents = null;
                }
            }
            lastGramsCount = 0;
        } else {
            updateBowlItemDuringConsumption();
        }
        needsSync = true;
    }

    // ─── Tick logic ───

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, HookahBlockEntity pBlockEntity) {
        if (pLevel.isClientSide()) return;

        // Puff each active connection
        boolean isAnyPuffing = false;
        Set<HookahConnectionEntity> connectionsToRemove = new HashSet<>();
        for (HookahConnectionEntity connection : new ArrayList<>(pBlockEntity.activeConnections)) {
            if (connection.isRemoved() || connection.level() != pLevel) {
                connectionsToRemove.add(connection);
                continue;
            }
            if (!connection.isPuffing()) continue;
            isAnyPuffing = true;

            UUID connUUID = connection.getAttachedPlayerUUID();
            Player player = connUUID != null ? pLevel.getPlayerByUUID(connUUID) : null;
            if (player instanceof ServerPlayer) {
                pBlockEntity.tryPuff(player);
            } else {
                connectionsToRemove.add(connection);
            }
        }
        // Чистим per-player аккумуляторы для удаляемых соединений, иначе мапы растут без границ
        for (HookahConnectionEntity removed : connectionsToRemove) {
            UUID uuid = removed.getAttachedPlayerUUID();
            if (uuid != null) {
                pBlockEntity.playerPuffDurations.remove(uuid);
                pBlockEntity.playerAirConsumed.remove(uuid);
                pBlockEntity.playerKaifAccumulators.remove(uuid);
                pBlockEntity.playerPuffEfficiencyAccumulators.remove(uuid);
            }
        }
        pBlockEntity.activeConnections.removeAll(connectionsToRemove);

        // Idle coal decay: when no one is puffing, coals cool down and burn through naturally
        if (!isAnyPuffing && pBlockEntity.golemPuffedAt != pLevel.getGameTime()
                && !pBlockEntity.zone.puffedWithin(pLevel.getGameTime(), HookahLogic.COALS_HOLD_HEAT_TICKS)) {
            pBlockEntity.idleCoalDecay();
        }

        if (pLevel.getGameTime() % net.sorukoma.quasium.magic.KaifZones.TICK_INTERVAL == 0
                && pLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            boolean smoldering = pBlockEntity.isSmoldering();
            pBlockEntity.zone.tick(serverLevel, pPos, smoldering, pBlockEntity instanceof ModularHookahBlockEntity,
                    pBlockEntity.zoneFlavor(), SmokeColorUtil.colorForContents(pBlockEntity.burningRow()));
            if (smoldering && Boolean.FALSE.equals(pBlockEntity.wasReady)) HookahSignals.ready(serverLevel, pBlockEntity.top());
            pBlockEntity.wasReady = smoldering;
        }

        if (!pBlockEntity.isHookahPuffDataDirty()) return;
        pBlockEntity.syncHookahPuffDataToTrackingClients();
    }

    private float smokerCooling() {
        return lastSmoker == null || level == null ? 1f : Perk.coolingMultiplier(level.getPlayerByUUID(lastSmoker));
    }

    private void idleCoalDecay() {
        ItemStack calloudStack = itemHandler.getStackInSlot(0);
        if (!(calloudStack.getItem() instanceof CalloudItem calloud) || calloud.burnsForever()) return;

        ModDataComponents.CalloudData calloudData = calloudStack.getOrDefault(
                ModDataComponents.CALLOUD_CONTENTS.get(),
                new ModDataComponents.CalloudData(List.of(), List.of())
        );

        if (calloudData.charcoals().isEmpty() && calloudData.stackedCharcoals().isEmpty()) return;

        List<ModDataComponents.CharcoalData> newCharcoals = new ArrayList<>();
        List<ModDataComponents.CharcoalData> newStackedCharcoals = new ArrayList<>();
        boolean anyChanged = false;

        for (ModDataComponents.CharcoalData cd : calloudData.charcoals()) {
            float newProgress = cd.heatingProgress();
            if (newProgress > 0f) {
                float coolingPerTick = 100.0f / (cd.size().getMaxTicks() * CALLOUD_IDLE_COOLING_FACTOR) * smokerCooling();
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
                float coolingPerTick = 100.0f / (cd.size().getMaxTicks() * CALLOUD_IDLE_COOLING_FACTOR) * smokerCooling();
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
            setChanged();
        }
    }

    // ─── Sync ───

    private boolean isHookahPuffDataDirty() {
        float currentHeat = getTotalHeat();
        int currentCharcoalCount = getCharcoalCount();
        return needsSync
                || Math.abs(lastSyncedTotalHeat - currentHeat) > 0.01f
                || lastSyncedCharcoalCount != currentCharcoalCount
                || lastSyncedTotalTobaccoWeight != totalTobaccoWeight
                || !java.util.Objects.equals(lastSyncedBowlContents, cachedBowlContents);
    }

    private PacketHookahPuffData createHookahPuffDataPacket() {
        return new PacketHookahPuffData(worldPosition, getTotalHeat(), totalTobaccoWeight, warmupPuffsRemaining, cachedBowlContents);
    }

    private void markHookahPuffDataSynced() {
        lastSyncedTotalHeat = getTotalHeat();
        lastSyncedCharcoalCount = getCharcoalCount();
        lastSyncedTotalTobaccoWeight = totalTobaccoWeight;
        lastSyncedBowlContents = cachedBowlContents;
        needsSync = false;
    }

    private void syncHookahPuffDataToTrackingClients() {
        setChanged();
        ModPackets.sendToClientsTrackingBlock(createHookahPuffDataPacket(), this);
        markHookahPuffDataSynced();
    }

    public void syncHookahPuffDataToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, createHookahPuffDataPacket());
    }

    // ─── NBT serialization ───

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
        pTag.putInt("TotalTobaccoWeight", totalTobaccoWeight);
        pTag.putInt("WarmupPuffsRemaining", warmupPuffsRemaining);
        pTag.putInt("FlaskCharges", flaskCharges);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        warmupPuffsRemaining = pTag.contains("WarmupPuffsRemaining") ? pTag.getInt("WarmupPuffsRemaining") : HookahLogic.WARMUP_PUFFS;
        flaskCharges = pTag.getInt("FlaskCharges");

        ItemStack slot1Item = itemHandler.getStackInSlot(1);
        if (!slot1Item.isEmpty() && slot1Item.is(ModItems.HOOKAH_BOWL.get())) {
            readBowlContents(slot1Item);
            if (pTag.contains("TotalTobaccoWeight")) {
                totalTobaccoWeight = Math.max(0, Math.min(pTag.getInt("TotalTobaccoWeight"),
                        cachedBowlContents == null ? 0 : cachedBowlContents.filledSlotCount() * TOBACCO_TICKS_PER_GRAM));
            }
        } else if (!slot1Item.isEmpty()) {
            LOGGER.warn("Legacy non-bowl item in slot 1 at {}, clearing", worldPosition);
            itemHandler.setStackInSlot(1, ItemStack.EMPTY);
            clearBowlState();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("TotalTobaccoWeight", totalTobaccoWeight);
        tag.putInt("WarmupPuffsRemaining", warmupPuffsRemaining);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // ─── Drops ───

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // ─── Menu provider ───

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.hookah");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new HookahMenu(pContainerId, pPlayerInventory, this);
    }
}
