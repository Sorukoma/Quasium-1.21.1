package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Map;

public record Spell(SpellForm form, List<Map<String, Integer>> inputs, int lungs, float grams, float cooldown,
                    int kaif, boolean hidden, String action, Map<String, Float> params, int color) {

    public static final Codec<Spell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpellForm.CODEC.fieldOf("form").forGetter(Spell::form),
            Codec.unboundedMap(Codec.STRING, ExtraCodecs.POSITIVE_INT).listOf().fieldOf("inputs").forGetter(Spell::inputs),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("lungs").forGetter(Spell::lungs),
            Codec.floatRange(0f, 64f).fieldOf("grams").forGetter(Spell::grams),
            Codec.FLOAT.optionalFieldOf("cooldown", 1f).forGetter(Spell::cooldown),
            Codec.INT.optionalFieldOf("kaif", 0).forGetter(Spell::kaif),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Spell::hidden),
            Codec.STRING.fieldOf("action").forGetter(Spell::action),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", Map.of()).forGetter(Spell::params),
            TextColor.CODEC.xmap(TextColor::getValue, TextColor::fromRgb).optionalFieldOf("color", 0xCCCCCC).forGetter(Spell::color)
    ).apply(instance, Spell::new));

    public float param(String key, float fallback) {
        Float value = params.get(key);
        return value == null ? fallback : value;
    }

    public int ticks(String key, float fallbackSeconds) {
        return Math.round(param(key, fallbackSeconds) * MagicMath.TICKS_PER_SECOND);
    }

    public int cooldownTicks() {
        return Math.round(cooldown * MagicMath.TICKS_PER_SECOND);
    }

    public static MutableComponent displayName(ResourceLocation id) {
        return Component.translatable("quasium.spell." + id.getPath());
    }
}
