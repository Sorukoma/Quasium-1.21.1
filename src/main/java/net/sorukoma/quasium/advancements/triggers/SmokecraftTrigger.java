package net.sorukoma.quasium.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.CriterionValidator;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SmokecraftTrigger extends SimpleCriterionTrigger<SmokecraftTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String event) {
        trigger(player, instance -> instance.event.equals(event));
    }

    public static void fire(ServerPlayer player, String event) {
        QuasiumModAdvancements.SMOKECRAFT.get().trigger(player, event);
    }

    public static Criterion<TriggerInstance> on(String event) {
        return new Criterion<>(QuasiumModAdvancements.SMOKECRAFT.get(), new TriggerInstance(Optional.empty(), event));
    }

    public static Criterion<TriggerInstance> on(String event, EntityPredicate.Builder player) {
        return new Criterion<>(QuasiumModAdvancements.SMOKECRAFT.get(), new TriggerInstance(Optional.of(EntityPredicate.wrap(player)), event));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String event) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("event").forGetter(TriggerInstance::event)
        ).apply(instance, TriggerInstance::new));

        @Override
        public void validate(CriterionValidator validator) {
            validator.validateEntity(player, ".player");
        }
    }
}
