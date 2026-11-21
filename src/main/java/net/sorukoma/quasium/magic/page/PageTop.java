package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Map;

public record PageTop(int volume, int max, int noteCost, Map<String, Float> scale, Map<String, Float> add,
                      List<String> forms, String unlock, int order) {
    public static final Codec<PageTop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("volume").forGetter(PageTop::volume),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("max", 1).forGetter(PageTop::max),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("note_cost", 0).forGetter(PageTop::noteCost),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("scale", Map.of()).forGetter(PageTop::scale),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("add", Map.of()).forGetter(PageTop::add),
            Codec.STRING.listOf().optionalFieldOf("forms", List.of()).forGetter(PageTop::forms),
            Codec.STRING.optionalFieldOf("unlock", "start").forGetter(PageTop::unlock),
            Codec.INT.optionalFieldOf("order", 0).forGetter(PageTop::order)
    ).apply(instance, PageTop::new));

    public boolean fits(String form) {
        return forms.isEmpty() || forms.contains(form);
    }
}
