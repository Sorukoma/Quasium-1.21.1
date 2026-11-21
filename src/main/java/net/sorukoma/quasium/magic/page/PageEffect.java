package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.NoteType;

import java.util.Map;

public record PageEffect(NoteType note, int volume, float cooldown, PageTarget target, Map<String, Float> params,
                         String unlock, int order) {
    public static final Codec<PageEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NoteType.CODEC.fieldOf("note").forGetter(PageEffect::note),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("volume").forGetter(PageEffect::volume),
            Codec.floatRange(0f, 600f).optionalFieldOf("cooldown", 1f).forGetter(PageEffect::cooldown),
            PageTarget.CODEC.optionalFieldOf("target", PageTarget.FOE).forGetter(PageEffect::target),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", Map.of()).forGetter(PageEffect::params),
            Codec.STRING.optionalFieldOf("unlock", "start").forGetter(PageEffect::unlock),
            Codec.INT.optionalFieldOf("order", 0).forGetter(PageEffect::order)
    ).apply(instance, PageEffect::new));

    public float param(String key, float fallback) {
        Float value = params.get(key);
        return value == null ? fallback : value;
    }

    public int cooldownTicks() {
        return Math.round(cooldown * MagicMath.TICKS_PER_SECOND);
    }
}
