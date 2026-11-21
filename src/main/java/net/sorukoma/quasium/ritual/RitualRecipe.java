package net.sorukoma.quasium.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.item.GearDurability;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.progression.ConfigArgs;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record RitualRecipe(int tier, Ingredient catalyst, List<Ingredient> pedestals, Map<NoteType, Integer> notes, int puffs,
                           int rate, KaifGate kaif, int base, Optional<ItemStack> result, DataComponentPatch infuse,
                           Optional<ResourceLocation> research) {
    public static final int MAX_TIER = 4;
    public static final int MAX_PEDESTALS = 8;

    public record KaifGate(Optional<Integer> min, Optional<Integer> orBelow) {
        public static final KaifGate ANY = new KaifGate(Optional.empty(), Optional.empty());
        public static final Codec<KaifGate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("min").forGetter(KaifGate::min),
                Codec.INT.optionalFieldOf("or_below").forGetter(KaifGate::orBelow)
        ).apply(instance, KaifGate::new));

        public boolean allows(float kaif) {
            if (min.isEmpty() && orBelow.isEmpty()) return true;
            return min.filter(floor -> kaif >= floor).isPresent() || orBelow.filter(ceiling -> kaif <= ceiling).isPresent();
        }
    }

    public static final Codec<RitualRecipe> CODEC = RecordCodecBuilder.<RitualRecipe>create(instance -> instance.group(
            Codec.intRange(1, MAX_TIER).fieldOf("tier").forGetter(RitualRecipe::tier),
            Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(RitualRecipe::catalyst),
            Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("pedestals", List.of()).forGetter(RitualRecipe::pedestals),
            Codec.unboundedMap(NoteType.CODEC, ExtraCodecs.POSITIVE_INT).optionalFieldOf("notes", Map.of()).forGetter(RitualRecipe::notes),
            ExtraCodecs.POSITIVE_INT.fieldOf("puffs").forGetter(RitualRecipe::puffs),
            ExtraCodecs.POSITIVE_INT.fieldOf("rate").forGetter(RitualRecipe::rate),
            KaifGate.CODEC.optionalFieldOf("kaif", KaifGate.ANY).forGetter(RitualRecipe::kaif),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("base").forGetter(RitualRecipe::base),
            ItemStack.CODEC.optionalFieldOf("result").forGetter(RitualRecipe::result),
            DataComponentPatch.CODEC.optionalFieldOf("infuse", DataComponentPatch.EMPTY).forGetter(RitualRecipe::infuse),
            ResourceLocation.CODEC.optionalFieldOf("research").forGetter(RitualRecipe::research)
    ).apply(instance, RitualRecipe::new)).validate(RitualRecipe::validate);

    private static DataResult<RitualRecipe> validate(RitualRecipe recipe) {
        if (recipe.result.isPresent() && !recipe.infuse.isEmpty()) {
            return DataResult.error(() -> "a ritual gives either a result or an infusion of the catalyst, not both");
        }
        if (recipe.pedestals.size() > MAX_PEDESTALS) {
            return DataResult.error(() -> "a ritual takes at most " + MAX_PEDESTALS + " pedestal items");
        }
        return DataResult.success(recipe);
    }

    public boolean takesCatalyst(ItemStack stack) {
        return !stack.isEmpty() && catalyst.test(stack) && !alreadyInfused(stack);
    }

    public boolean alreadyInfused(ItemStack stack) {
        if (infuse.isEmpty()) return false;
        for (Map.Entry<DataComponentType<?>, Optional<?>> change : infuse.entrySet()) {
            if (!Objects.equals(stack.get(change.getKey()), change.getValue().orElse(null))) return false;
        }
        return true;
    }

    public boolean needsNirvana() {
        return tier >= ConfigArgs.synced(RitualConfig.NIRVANA_TIER);
    }

    public int work() {
        return RitualMath.workForPuffs(puffs, ConfigArgs.synced(RitualConfig.HITS_PER_100_PUFFS));
    }

    public int cleanHits() {
        return RitualMath.cleanHits(work(), ConfigArgs.synced(RitualConfig.STREAK_HITS), ConfigArgs.synced(RitualConfig.STREAK_MAX_PCT));
    }

    public int pulseMs() {
        return RitualConfig.pulseMs(tier);
    }

    public int windowMs() {
        return RitualConfig.windowMs(tier);
    }

    public ItemStack craft(ItemStack catalystStack) {
        if (result.isPresent()) return result.get().copy();
        if (infuse.isEmpty()) return ItemStack.EMPTY;
        ItemStack infused = catalystStack.copyWithCount(1);
        infused.applyComponents(infuse);
        GearDurability.refresh(infused);
        return infused;
    }

    public ItemStack preview() {
        ItemStack[] options = catalyst.getItems();
        return previewFor(options.length == 0 ? ItemStack.EMPTY : options[0]);
    }

    public ItemStack previewFor(ItemStack shownCatalyst) {
        if (result.isPresent()) return result.get().copy();
        return shownCatalyst.isEmpty() ? ItemStack.EMPTY : craft(shownCatalyst);
    }

    @Nullable
    public int[] matchPedestals(List<ItemStack> offered) {
        int[] owners = pedestalOwners(offered);
        int[] picked = new int[pedestals.size()];
        Arrays.fill(picked, -1);
        for (int index = 0; index < owners.length; index++) {
            if (owners[index] >= 0) picked[owners[index]] = index;
        }
        return Arrays.stream(picked).allMatch(index -> index >= 0) ? picked : null;
    }

    public List<Integer> missingPedestals(List<ItemStack> offered) {
        Set<Integer> placed = new HashSet<>();
        for (int owner : pedestalOwners(offered)) placed.add(owner);
        List<Integer> missing = new ArrayList<>();
        for (int slot = 0; slot < pedestals.size(); slot++) {
            if (!placed.contains(slot)) missing.add(slot);
        }
        return missing;
    }

    private int[] pedestalOwners(List<ItemStack> offered) {
        int[] owners = new int[offered.size()];
        Arrays.fill(owners, -1);
        for (int slot = 0; slot < pedestals.size(); slot++) claim(slot, owners, new boolean[offered.size()], offered);
        return owners;
    }

    private boolean claim(int slot, int[] owners, boolean[] tried, List<ItemStack> offered) {
        for (int index = 0; index < offered.size(); index++) {
            if (tried[index] || offered.get(index).isEmpty() || !pedestals.get(slot).test(offered.get(index))) continue;
            tried[index] = true;
            if (owners[index] < 0 || claim(owners[index], owners, tried, offered)) {
                owners[index] = slot;
                return true;
            }
        }
        return false;
    }
}
