package net.sorukoma.quasium.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.CriterionValidator;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.item.TobaccoItem;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PureTobaccoSmokedTrigger extends SimpleCriterionTrigger<PureTobaccoSmokedTrigger.TriggerInstance> {
    private static final String SELF_MIXED = "self_mixed_";

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String tobacco) {
        trigger(player, instance -> instance.tobacco.equals(tobacco));
    }

    public static void fire(ServerPlayer player, HookahBowlContents bowl) {
        pureKind(bowl).ifPresent(kind -> QuasiumModAdvancements.PURE_TOBACCO_SMOKED.get().trigger(player, kind));
    }

    public static Criterion<TriggerInstance> smoked(String kind) {
        return new Criterion<>(QuasiumModAdvancements.PURE_TOBACCO_SMOKED.get(), new TriggerInstance(Optional.empty(), kind));
    }

    public static List<String> kinds() {
        Stream<String> flavors = BuiltInRegistries.ITEM.stream()
                .filter(TobaccoItem.class::isInstance).map(TobaccoItem.class::cast).map(TobaccoItem::getFlavor);
        Stream<String> varieties = BuiltInRegistries.BLOCK.stream()
                .filter(TobaccoCropBlock.class::isInstance).map(TobaccoCropBlock.class::cast).map(TobaccoCropBlock::getVariety)
                .map(variety -> SELF_MIXED + variety);
        return Stream.concat(flavors, varieties).distinct().toList();
    }

    public static Optional<String> pureKind(HookahBowlContents bowl) {
        Set<String> kinds = bowl.slotAssignments().values().stream()
                .map(group -> kindOf(bowl.groups().get(group)))
                .collect(Collectors.toSet());
        return kinds.size() == 1 ? kinds.stream().findFirst() : Optional.empty();
    }

    private static String kindOf(BowlTobaccoGroup group) {
        return group.isRegular() ? group.flavor() : SELF_MIXED + group.base();
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String tobacco) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("tobacco").forGetter(TriggerInstance::tobacco)
        ).apply(instance, TriggerInstance::new));

        @Override
        public void validate(CriterionValidator validator) {
            validator.validateEntity(player, ".player");
        }
    }
}
