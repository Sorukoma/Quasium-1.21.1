package net.sorukoma.quasium.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, QuasiumMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> HEAVY = EFFECTS.register("heavy",
            () -> new SmokeEffect(MobEffectCategory.HARMFUL, 0x5A4A3A)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, id("heavy_jump"), -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> DROWSY = EFFECTS.register("drowsy",
            () -> new SmokeEffect(MobEffectCategory.HARMFUL, 0xB06CCF)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("drowsy_speed"), -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.FOLLOW_RANGE, id("drowsy_range"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> ECHO_MUFFLE = EFFECTS.register("echo_muffle",
            () -> new SmokeEffect(MobEffectCategory.BENEFICIAL, 0x29DFEB));

    public static final DeferredHolder<MobEffect, MobEffect> SNACKED = EFFECTS.register("snacked",
            () -> new SmokeEffect(MobEffectCategory.BENEFICIAL, 0xE0A030));

    private ModEffects() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }

    private static final class SmokeEffect extends MobEffect {
        private SmokeEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
