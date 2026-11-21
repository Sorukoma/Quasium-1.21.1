package net.sorukoma.quasium.mind.dream;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindRegistry;

import java.util.Locale;

public enum DreamOutcome {
    INSPIRATION(MindConfig.WEIGHT_INSPIRATION, MindRegistry.DREAM_INSPIRATION),
    COMPASS(MindConfig.WEIGHT_COMPASS, MindRegistry.DREAM_COMPASS),
    RECIPE(MindConfig.WEIGHT_RECIPE, MindRegistry.DREAM_RECIPE),
    INSIGHT(MindConfig.WEIGHT_INSIGHT, MindRegistry.DREAM_INSIGHT),
    LUCID(MindConfig.WEIGHT_LUCID, MindRegistry.DREAM_LUCID);

    private final ModConfigSpec.IntValue weight;
    private final DeferredHolder<SoundEvent, SoundEvent> sound;

    DreamOutcome(ModConfigSpec.IntValue weight, DeferredHolder<SoundEvent, SoundEvent> sound) {
        this.weight = weight;
        this.sound = sound;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("quasium.dream." + id());
    }

    int weight(boolean controlled) {
        return this == LUCID && controlled ? MindConfig.WEIGHT_LUCID_CONTROLLED.getAsInt() : weight.getAsInt();
    }

    SoundEvent sound() {
        return sound.get();
    }
}
