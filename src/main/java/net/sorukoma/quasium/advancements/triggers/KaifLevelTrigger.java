package net.sorukoma.quasium.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.QuasiumMod;
import net.minecraft.advancements.critereon.CriterionValidator;

import java.util.Optional;

public class KaifLevelTrigger extends SimpleCriterionTrigger<KaifLevelTrigger.TriggerInstance> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "kaif_level");

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int currentKaif) {
        this.trigger(player, instance -> instance.matches(currentKaif));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, int minKaifLevel) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.INT.fieldOf("min_kaif_level").forGetter(TriggerInstance::minKaifLevel)
        ).apply(instance, TriggerInstance::new));

        // Удален статический метод kaifLevel

        public boolean matches(int currentKaif) {
            return currentKaif >= this.minKaifLevel;
        }

        @Override
        public void validate(CriterionValidator validator) {
            validator.validateEntity(this.player, ".player");
        }
    }
}
