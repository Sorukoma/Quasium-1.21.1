package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.TextColor;
import net.sorukoma.quasium.magic.NoteType;

import java.util.Map;

public record Accord(NoteType first, NoteType second, Map<String, Float> params, int color) {
    public static final Codec<Accord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NoteType.CODEC.fieldOf("first").forGetter(Accord::first),
            NoteType.CODEC.fieldOf("second").forGetter(Accord::second),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", Map.of()).forGetter(Accord::params),
            TextColor.CODEC.xmap(TextColor::getValue, TextColor::fromRgb).optionalFieldOf("color", 0xCCCCCC).forGetter(Accord::color)
    ).apply(instance, Accord::new));

    public boolean joins(NoteType a, NoteType b) {
        return (first == a && second == b) || (first == b && second == a);
    }

    public float param(String key, float fallback) {
        Float value = params.get(key);
        return value == null ? fallback : value;
    }
}
