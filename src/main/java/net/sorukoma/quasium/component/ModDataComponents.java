package net.sorukoma.quasium.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.QuasiumMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, QuasiumMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KAIF_VALUE = DATA_COMPONENT_TYPES.register("kaif_value",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOBACCO_WEIGHT = DATA_COMPONENT_TYPES.register("tobacco_weight",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> HOOKAH_PIPE_COOLDOWN_END = DATA_COMPONENT_TYPES.register("hookah_pipe_cooldown_end",
            () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> TOBACCO_DAMAGE = DATA_COMPONENT_TYPES.register("tobacco_damage",
            () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FERMENTED = DATA_COMPONENT_TYPES.register("fermented",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SPOILED = DATA_COMPONENT_TYPES.register("spoiled",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MobEffectInstance>> POTION_EFFECT = DATA_COMPONENT_TYPES.register("potion_effect",
            () -> DataComponentType.<MobEffectInstance>builder()
                    .persistent(MobEffectInstance.CODEC)
                    .networkSynchronized(MobEffectInstance.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MOLASSES_AMOUNT = DATA_COMPONENT_TYPES.register("molasses_amount",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MOLASSES_MASS = DATA_COMPONENT_TYPES.register("molasses_mass",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DRYING_STAGE = DATA_COMPONENT_TYPES.register("drying_stage",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DRYING_PROGRESS = DATA_COMPONENT_TYPES.register("drying_progress",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DRYING_RATE = DATA_COMPONENT_TYPES.register("drying_rate",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ESSENCE_TYPE = DATA_COMPONENT_TYPES.register("essence_type",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ESSENCE_AMOUNT = DATA_COMPONENT_TYPES.register("essence_amount",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TOXIC = DATA_COMPONENT_TYPES.register("toxic",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOBACCO_BASE = DATA_COMPONENT_TYPES.register("tobacco_base",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<MobEffectInstance>>> SELF_MIXED_EFFECTS = DATA_COMPONENT_TYPES.register("self_mixed_effects",
            () -> DataComponentType.<List<MobEffectInstance>>builder()
                    .persistent(MobEffectInstance.CODEC.listOf())
                    .networkSynchronized(StreamCodec.of(
                            (buf, list) -> {
                                buf.writeVarInt(list.size());
                                for (MobEffectInstance effect : list) {
                                    MobEffectInstance.STREAM_CODEC.encode(buf, effect);
                                }
                            },
                            buf -> {
                                int size = buf.readVarInt();
                                List<MobEffectInstance> list = new ArrayList<>();
                                for (int i = 0; i < size; i++) {
                                    list.add(MobEffectInstance.STREAM_CODEC.decode(buf));
                                }
                                return Collections.unmodifiableList(list);
                            }
                    ))
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HEATED_MARKER = DATA_COMPONENT_TYPES.register("heated_marker",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOBACCO_FLAVOR = DATA_COMPONENT_TYPES.register("tobacco_flavor",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOBACCO_VARIETY = DATA_COMPONENT_TYPES.register("tobacco_variety",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOBACCO_STRENGTH_STARS = DATA_COMPONENT_TYPES.register("tobacco_strength_stars",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOBACCO_YIELD_STARS = DATA_COMPONENT_TYPES.register("tobacco_yield_stars",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOBACCO_QUALITY_STARS = DATA_COMPONENT_TYPES.register("tobacco_quality_stars",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOBACCO_MUTATION = DATA_COMPONENT_TYPES.register("tobacco_mutation",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HookahBowlContents>> HOOKAH_BOWL_CONTENTS = DATA_COMPONENT_TYPES.register("hookah_bowl_contents",
            () -> DataComponentType.<HookahBowlContents>builder()
                    .persistent(HookahBowlContents.CODEC)
                    .networkSynchronized(HookahBowlContents.STREAM_CODEC)
                    .build());

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }

    public record BowlTobaccoGroup(
            String type,       // "regular" or "self_mixed"
            String flavor,     // для regular: flavor name; для self_mixed: group identifier
            String base,       // для self_mixed: "tobacco"/"makhorka"; для regular: "regular"
            float strength,    // крепость; 0.0 для regular
            List<MobEffectInstance> effects,  // эффекты self_mixed; empty для regular
            int essenceAmount,
            int molassesAmount,
            boolean toxic,     // токсичный самозамес (мутация гибрида) — наносит урон при курении
            int kaifPct
    ) {
        public BowlTobaccoGroup(String type, String flavor, String base, float strength,
                                List<MobEffectInstance> effects) {
            this(type, flavor, base, strength, effects, 0, 0, false, 100);
        }

        public BowlTobaccoGroup(String type, String flavor, String base, float strength,
                                List<MobEffectInstance> effects, int essenceAmount, int molassesAmount) {
            this(type, flavor, base, strength, effects, essenceAmount, molassesAmount, false, 100);
        }

        public BowlTobaccoGroup {
            effects = Collections.unmodifiableList(new ArrayList<>(effects));
        }

        public boolean isRegular() { return "regular".equals(type); }
        public boolean isSelfMixed() { return "self_mixed".equals(type); }

        public static final Codec<BowlTobaccoGroup> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("type").forGetter(BowlTobaccoGroup::type),
                        Codec.STRING.fieldOf("flavor").forGetter(BowlTobaccoGroup::flavor),
                        Codec.STRING.fieldOf("base").forGetter(BowlTobaccoGroup::base),
                        Codec.FLOAT.fieldOf("strength").forGetter(BowlTobaccoGroup::strength),
                        MobEffectInstance.CODEC.listOf().fieldOf("effects").forGetter(BowlTobaccoGroup::effects),
                        Codec.INT.optionalFieldOf("essence_amount", 0).forGetter(BowlTobaccoGroup::essenceAmount),
                        Codec.INT.optionalFieldOf("molasses_amount", 0).forGetter(BowlTobaccoGroup::molassesAmount),
                        Codec.BOOL.optionalFieldOf("toxic", false).forGetter(BowlTobaccoGroup::toxic),
                        Codec.INT.optionalFieldOf("kaif_pct", 100).forGetter(BowlTobaccoGroup::kaifPct)
                ).apply(instance, BowlTobaccoGroup::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BowlTobaccoGroup> STREAM_CODEC = StreamCodec.of(
                (buf, group) -> {
                    buf.writeUtf(group.type());
                    buf.writeUtf(group.flavor());
                    buf.writeUtf(group.base());
                    buf.writeFloat(group.strength());
                    buf.writeVarInt(group.effects().size());
                    for (MobEffectInstance effect : group.effects()) {
                        MobEffectInstance.STREAM_CODEC.encode(buf, effect);
                    }
                    buf.writeVarInt(group.essenceAmount());
                    buf.writeVarInt(group.molassesAmount());
                    buf.writeBoolean(group.toxic());
                    buf.writeVarInt(group.kaifPct());
                },
                buf -> {
                    String type = buf.readUtf();
                    String flavor = buf.readUtf();
                    String base = buf.readUtf();
                    float strength = buf.readFloat();
                    int effectCount = buf.readVarInt();
                    List<MobEffectInstance> effects = new ArrayList<>();
                    for (int i = 0; i < effectCount; i++) {
                        effects.add(MobEffectInstance.STREAM_CODEC.decode(buf));
                    }
                    return new BowlTobaccoGroup(type, flavor, base, strength, effects,
                            buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
                }
        );

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BowlTobaccoGroup that = (BowlTobaccoGroup) o;
            return Objects.equals(type, that.type) && Objects.equals(flavor, that.flavor)
                    && Objects.equals(base, that.base) && Float.compare(strength, that.strength) == 0
                    && essenceAmount == that.essenceAmount && molassesAmount == that.molassesAmount
                    && toxic == that.toxic && kaifPct == that.kaifPct
                    && Objects.equals(effects, that.effects);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, flavor, base, strength, effects, essenceAmount, molassesAmount, toxic, kaifPct);
        }
    }

    public record HookahBowlContents(
            List<BowlTobaccoGroup> groups,
            Map<Integer, Integer> slotAssignments,  // slot index (0..19) -> group index (active tobacco)
            Set<Integer> burnedSlots,               // slots containing burned tobacco residue
            boolean heated                          // bowl has been heated in a hookah — active tobacco cannot be retrieved
    ) {
        public static final int MAX_SLOTS = 20;

        public HookahBowlContents {
            groups = Collections.unmodifiableList(new ArrayList<>(groups));
            slotAssignments = Collections.unmodifiableMap(new TreeMap<>(slotAssignments));
            burnedSlots = Collections.unmodifiableSet(new TreeSet<>(burnedSlots));
        }

        public int filledSlotCount() {
            return slotAssignments.size();
        }

        public int totalOccupiedSlots() {
            return slotAssignments.size() + burnedSlots.size();
        }

        public int totalWeightGrams() {
            return filledSlotCount();
        }

        /** Проверка совместимости табаков в чаше.
         *  Compatible: все regular, или все self_mixed с одинаковой base.
         *  Incompatible: mixed types, или self_mixed с разными bases.
         *  Burned slots are neutral — don't affect compatibility. */
        public boolean isCompatible() {
            if (slotAssignments.isEmpty()) return true;
            Set<String> types = new HashSet<>();
            Set<String> bases = new HashSet<>();
            for (Map.Entry<Integer, Integer> entry : slotAssignments.entrySet()) {
                BowlTobaccoGroup group = groups.get(entry.getValue());
                types.add(group.type());
                if (group.isSelfMixed()) bases.add(group.base());
            }
            if (types.size() > 1) return false;
            if (types.contains("self_mixed") && bases.size() > 1) return false;
            return true;
        }

        /** Получить вес каждой группы в граммах. */
        public Map<Integer, Integer> getGroupWeights() {
            Map<Integer, Integer> weights = new LinkedHashMap<>();
            for (int groupIdx : slotAssignments.values()) {
                weights.merge(groupIdx, 1, Integer::sum);
            }
            return weights;
        }

        /** Получить процент каждой группы. */
        public Map<Integer, Integer> getGroupPercentages() {
            Map<Integer, Integer> weights = getGroupWeights();
            int totalGrams = weights.values().stream().mapToInt(Integer::intValue).sum();
            if (totalGrams == 0) return Map.of();
            Map<Integer, Integer> percentages = new LinkedHashMap<>();
            for (Map.Entry<Integer, Integer> entry : weights.entrySet()) {
                int pct = (int) Math.round(entry.getValue() * 100.0 / totalGrams);
                percentages.merge(entry.getKey(), pct, Integer::sum);
            }
            int sum = percentages.values().stream().mapToInt(Integer::intValue).sum();
            if (sum != 100 && !percentages.isEmpty()) {
                int largestIdx = percentages.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(percentages.keySet().iterator().next());
                percentages.put(largestIdx, percentages.get(largestIdx) + (100 - sum));
            }
            return percentages;
        }

        /** Convert the last active slot to a burned slot (tobacco fully consumed). */
        public HookahBowlContents burnLastSlot() {
            if (slotAssignments.isEmpty()) return this;
            int lastSlot = slotAssignments.keySet().stream().max(Integer::compareTo).orElse(-1);
            Map<Integer, Integer> newAssignments = new TreeMap<>(slotAssignments);
            newAssignments.remove(lastSlot);
            Set<Integer> newBurnedSlots = new TreeSet<>(burnedSlots);
            newBurnedSlots.add(lastSlot);
            List<BowlTobaccoGroup> newGroups = compactGroups(new ArrayList<>(groups), newAssignments);
            return new HookahBowlContents(newGroups, newAssignments, newBurnedSlots, heated);
        }

        /** Remove a burned slot (player takes out burned tobacco residue). */
        public HookahBowlContents removeBurnedSlot(int slotIndex) {
            if (!burnedSlots.contains(slotIndex)) return this;
            Set<Integer> newBurnedSlots = new TreeSet<>(burnedSlots);
            newBurnedSlots.remove(slotIndex);
            return new HookahBowlContents(groups, slotAssignments, newBurnedSlots, heated);
        }

        /** Mark the bowl as heated (placed in a hookah). */
        public HookahBowlContents withHeated(boolean heated) {
            return new HookahBowlContents(groups, slotAssignments, burnedSlots, heated);
        }

        /** Completely clear the bowl (empty state, unheated). */
        public static HookahBowlContents cleared() {
            return new HookahBowlContents(List.of(), Map.of(), Set.of(), false);
        }

        public HookahBowlContents removeLastSlot() {
            if (slotAssignments.isEmpty()) return this;
            int lastSlot = slotAssignments.keySet().stream().max(Integer::compareTo).orElse(-1);
            Map<Integer, Integer> newAssignments = new TreeMap<>(slotAssignments);
            newAssignments.remove(lastSlot);
            List<BowlTobaccoGroup> newGroups = compactGroups(new ArrayList<>(groups), newAssignments);
            return new HookahBowlContents(newGroups, newAssignments, burnedSlots, heated);
        }

        /** Удалить группы, на которые не ссылается ни один слот, и пересчитать индексы. */
        private static List<BowlTobaccoGroup> compactGroups(List<BowlTobaccoGroup> groups, Map<Integer, Integer> assignments) {
            Set<Integer> usedIndices = new HashSet<>(assignments.values());
            if (usedIndices.size() == groups.size()) return groups; // no compaction needed

            // Build index mapping: old index -> new index
            Map<Integer, Integer> indexMap = new LinkedHashMap<>();
            int newIndex = 0;
            for (int oldIndex = 0; oldIndex < groups.size(); oldIndex++) {
                if (usedIndices.contains(oldIndex)) {
                    indexMap.put(oldIndex, newIndex++);
                }
            }

            // Remap assignments
            Map<Integer, Integer> remappedAssignments = new TreeMap<>();
            for (Map.Entry<Integer, Integer> entry : assignments.entrySet()) {
                remappedAssignments.put(entry.getKey(), indexMap.get(entry.getValue()));
            }
            assignments.clear();
            assignments.putAll(remappedAssignments);

            // Build new groups list
            List<BowlTobaccoGroup> newGroups = new ArrayList<>();
            for (int oldIndex = 0; oldIndex < groups.size(); oldIndex++) {
                if (usedIndices.contains(oldIndex)) {
                    newGroups.add(groups.get(oldIndex));
                }
            }
            return newGroups;
        }

        /** Найти или создать группу для заданного табачного предмета. */
        public HookahBowlContents addSlot(int slotIndex, BowlTobaccoGroup groupData) {
            // Find matching group
            int existingGroupIdx = -1;
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i).equals(groupData)) {
                    existingGroupIdx = i;
                    break;
                }
            }

            List<BowlTobaccoGroup> newGroups;
            int groupIdx;
            if (existingGroupIdx >= 0) {
                newGroups = new ArrayList<>(groups);
                groupIdx = existingGroupIdx;
            } else {
                newGroups = new ArrayList<>(groups);
                newGroups.add(groupData);
                groupIdx = newGroups.size() - 1;
            }

            Map<Integer, Integer> newAssignments = new TreeMap<>(slotAssignments);
            newAssignments.put(slotIndex, groupIdx);
            return new HookahBowlContents(newGroups, newAssignments, burnedSlots, heated);
        }

        /** Удалить конкретный слот. */
        public HookahBowlContents removeSlot(int slotIndex) {
            Map<Integer, Integer> newAssignments = new TreeMap<>(slotAssignments);
            newAssignments.remove(slotIndex);
            Set<Integer> newBurnedSlots = new TreeSet<>(burnedSlots);
            newBurnedSlots.remove(slotIndex);
            List<BowlTobaccoGroup> newGroups = compactGroups(new ArrayList<>(groups), newAssignments);
            return new HookahBowlContents(newGroups, newAssignments, newBurnedSlots, heated);
        }

        public static final Codec<HookahBowlContents> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BowlTobaccoGroup.CODEC.listOf().fieldOf("groups").forGetter(HookahBowlContents::groups),
                        Codec.INT.listOf().xmap(
                                (List<Integer> list) -> {
                                    Map<Integer, Integer> map = new TreeMap<>();
                                    for (int i = 0; i < list.size(); i += 2) {
                                        map.put(list.get(i), list.get(i + 1));
                                    }
                                    return map;
                                },
                                (Map<Integer, Integer> map) -> {
                                    List<Integer> list = new ArrayList<>();
                                    for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                                        list.add(entry.getKey());
                                        list.add(entry.getValue());
                                    }
                                    return list;
                                }
                        ).fieldOf("slotAssignments").forGetter(HookahBowlContents::slotAssignments),
                        Codec.INT.listOf().xmap((List<Integer> list) -> (Set<Integer>) new TreeSet<>(list), (Set<Integer> set) -> new ArrayList<>(set)).fieldOf("burnedSlots").forGetter(c -> c.burnedSlots()),
                        Codec.BOOL.fieldOf("heated").forGetter(HookahBowlContents::heated)
                ).apply(instance, HookahBowlContents::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, HookahBowlContents> STREAM_CODEC = StreamCodec.of(
                (buf, data) -> {
                    buf.writeVarInt(data.groups.size());
                    for (BowlTobaccoGroup group : data.groups) {
                        BowlTobaccoGroup.STREAM_CODEC.encode(buf, group);
                    }
                    buf.writeVarInt(data.slotAssignments.size());
                    for (Map.Entry<Integer, Integer> entry : data.slotAssignments.entrySet()) {
                        buf.writeVarInt(entry.getKey());
                        buf.writeVarInt(entry.getValue());
                    }
                    buf.writeVarInt(data.burnedSlots.size());
                    for (int slot : data.burnedSlots) {
                        buf.writeVarInt(slot);
                    }
                    buf.writeBoolean(data.heated);
                },
                buf -> {
                    int groupCount = buf.readVarInt();
                    List<BowlTobaccoGroup> groups = new ArrayList<>();
                    for (int i = 0; i < groupCount; i++) {
                        groups.add(BowlTobaccoGroup.STREAM_CODEC.decode(buf));
                    }
                    int slotCount = buf.readVarInt();
                    Map<Integer, Integer> assignments = new TreeMap<>();
                    for (int i = 0; i < slotCount; i++) {
                        assignments.put(buf.readVarInt(), buf.readVarInt());
                    }
                    int burnedCount = buf.readVarInt();
                    Set<Integer> burnedSlots = new TreeSet<>();
                    for (int i = 0; i < burnedCount; i++) {
                        burnedSlots.add(buf.readVarInt());
                    }
                    boolean heated = buf.readBoolean();
                    return new HookahBowlContents(groups, assignments, burnedSlots, heated);
                }
        );

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HookahBowlContents that = (HookahBowlContents) o;
            return Objects.equals(groups, that.groups) && Objects.equals(slotAssignments, that.slotAssignments)
                    && Objects.equals(burnedSlots, that.burnedSlots) && heated == that.heated;
        }

        @Override
        public int hashCode() {
            return Objects.hash(groups, slotAssignments, burnedSlots, heated);
        }
    }

    // ─── Charcoal & Calloud Components ───

    public enum CharcoalSize {
        MM_22("22mm", 3, 1.0f, 3600),
        MM_25("25mm", 4, 1.5f, 6000),
        MM_27("27mm", 6, 2.0f, 9600);

        private final String name;
        private final int volume; // для расчета вместимости каллауда (макс 12)
        private final float maxHeat;
        private final int maxTicks;

        CharcoalSize(String name, int volume, float maxHeat, int maxTicks) {
            this.name = name;
            this.volume = volume;
            this.maxHeat = maxHeat;
            this.maxTicks = maxTicks;
        }

        public String getName() { return name; }
        public int getVolume() { return volume; }
        public float getMaxHeat() { return maxHeat; }
        public int getMaxTicks() { return maxTicks; }

        public static final Codec<CharcoalSize> CODEC = Codec.STRING.xmap(
                s -> valueOf(s),
                CharcoalSize::name
        );
    }

    public record CharcoalData(
            CharcoalSize size,
            int remainingTicks,
            float heatingProgress // 0.0-100.0
    ) {
        public static final Codec<CharcoalData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        CharcoalSize.CODEC.fieldOf("size").forGetter(CharcoalData::size),
                        Codec.INT.fieldOf("remainingTicks").forGetter(CharcoalData::remainingTicks),
                        Codec.FLOAT.fieldOf("heatingProgress").forGetter(CharcoalData::heatingProgress)
                ).apply(instance, CharcoalData::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, CharcoalData> STREAM_CODEC = StreamCodec.of(
                (buf, data) -> {
                    buf.writeUtf(data.size.name());
                    buf.writeVarInt(data.remainingTicks);
                    buf.writeFloat(data.heatingProgress);
                },
                buf -> new CharcoalData(
                        CharcoalSize.valueOf(buf.readUtf()),
                        buf.readVarInt(),
                        buf.readFloat()
                )
        );

        public float getCurrentHeat() {
            if (remainingTicks <= 0 || heatingProgress <= 0f) return 0f;
            return size.getMaxHeat() * (heatingProgress / 100.0f);
        }
    }

    public record CalloudData(
            List<CharcoalData> charcoals,
            List<CharcoalData> stackedCharcoals
    ) {
        public static final float STACKED_HEAT_MULTIPLIER = 0.5f; // Stacked coals give 50% of normal heat

        public CalloudData {
            charcoals = Collections.unmodifiableList(new ArrayList<>(charcoals));
            stackedCharcoals = Collections.unmodifiableList(new ArrayList<>(stackedCharcoals));
        }

        public int totalVolume() {
            return charcoals.stream().mapToInt(c -> c.size().getVolume()).sum();
        }

        public int charcoalCount() {
            return charcoals.size() + stackedCharcoals.size();
        }

        public float totalHeat() {
            float sum = 0f;
            for (CharcoalData c : charcoals) {
                sum += c.getCurrentHeat();
            }
            for (CharcoalData c : stackedCharcoals) {
                sum += c.getCurrentHeat() * STACKED_HEAT_MULTIPLIER;
            }
            return sum;
        }

        public float maxPossibleHeat() {
            float sum = 0f;
            for (CharcoalData c : charcoals) {
                sum += c.size().getMaxHeat();
            }
            for (CharcoalData c : stackedCharcoals) {
                sum += c.size().getMaxHeat() * STACKED_HEAT_MULTIPLIER;
            }
            return sum;
        }

        public static final Codec<CalloudData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        CharcoalData.CODEC.listOf().fieldOf("charcoals").forGetter(CalloudData::charcoals),
                        CharcoalData.CODEC.listOf().fieldOf("stackedCharcoals").forGetter(CalloudData::stackedCharcoals)
                ).apply(instance, CalloudData::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, CalloudData> STREAM_CODEC = StreamCodec.of(
                (buf, data) -> {
                    buf.writeVarInt(data.charcoals.size());
                    for (CharcoalData c : data.charcoals) {
                        CharcoalData.STREAM_CODEC.encode(buf, c);
                    }
                    buf.writeVarInt(data.stackedCharcoals.size());
                    for (CharcoalData c : data.stackedCharcoals) {
                        CharcoalData.STREAM_CODEC.encode(buf, c);
                    }
                },
                buf -> {
                    int count = buf.readVarInt();
                    List<CharcoalData> charcoals = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        charcoals.add(CharcoalData.STREAM_CODEC.decode(buf));
                    }
                    int stackedCount = buf.readVarInt();
                    List<CharcoalData> stacked = new ArrayList<>();
                    for (int i = 0; i < stackedCount; i++) {
                        stacked.add(CharcoalData.STREAM_CODEC.decode(buf));
                    }
                    return new CalloudData(charcoals, stacked);
                }
        );
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CalloudData>> CALLOUD_CONTENTS = DATA_COMPONENT_TYPES.register("calloud_contents",
            () -> DataComponentType.<CalloudData>builder()
                    .persistent(CalloudData.CODEC)
                    .networkSynchronized(CalloudData.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CharcoalData>> CHARCOAL_DATA = DATA_COMPONENT_TYPES.register("charcoal_data",
            () -> DataComponentType.<CharcoalData>builder()
                    .persistent(CharcoalData.CODEC)
                    .networkSynchronized(CharcoalData.STREAM_CODEC)
                    .build());

    // ─── Модульный кальян ───
    // Вариант (цвет) колбы/шахты хранится строкой на предмете и в BlockEntity.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> HOOKAH_BULB_VARIANT = DATA_COMPONENT_TYPES.register("hookah_bulb_variant",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> HOOKAH_SHAFT_VARIANT = DATA_COMPONENT_TYPES.register("hookah_shaft_variant",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NOTE_INFUSION = DATA_COMPONENT_TYPES.register("note_infusion",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BOWL_UPGRADE = DATA_COMPONENT_TYPES.register("bowl_upgrade",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<net.sorukoma.quasium.magic.FocusEssence>>> BOWL_FOCUS = DATA_COMPONENT_TYPES.register("bowl_focus",
            () -> DataComponentType.<List<net.sorukoma.quasium.magic.FocusEssence>>builder()
                    .persistent(net.sorukoma.quasium.magic.FocusEssence.LIST_CODEC)
                    .networkSynchronized(net.sorukoma.quasium.magic.FocusEssence.LIST_STREAM_CODEC)
                    .build());
}
