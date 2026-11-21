package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.sorukoma.quasium.magic.SpellForm;

import java.util.Map;

public record PageForm(SpellForm shape, int volume, Map<String, Float> params, String unlock, int order) {
    public static final Codec<PageForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpellForm.CODEC.fieldOf("shape").forGetter(PageForm::shape),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("volume").forGetter(PageForm::volume),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", Map.of()).forGetter(PageForm::params),
            Codec.STRING.optionalFieldOf("unlock", "start").forGetter(PageForm::unlock),
            Codec.INT.optionalFieldOf("order", 0).forGetter(PageForm::order)
    ).apply(instance, PageForm::new));
}
