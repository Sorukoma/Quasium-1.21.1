package net.sorukoma.quasium.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.CriterionValidator;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Optional;

public class HookahOpenedTrigger extends SimpleCriterionTrigger<HookahOpenedTrigger.TriggerInstance> {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hookah_opened");

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));

        @Override
        public void validate(CriterionValidator validator) {
            validator.validateEntity(this.player, ".player");
        }
    }
}