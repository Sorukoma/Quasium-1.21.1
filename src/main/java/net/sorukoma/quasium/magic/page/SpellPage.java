package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record SpellPage(String name, String form, List<String> hearts, List<String> tops, String overflow, String seal) {
    public static final int MAX_NAME = 32;
    public static final int MAX_HEARTS = 3;
    public static final int MAX_TOPS = 4;
    public static final SpellPage EMPTY = new SpellPage("", "", List.of(), List.of(), "", "");

    public static final Codec<SpellPage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.string(0, MAX_NAME).optionalFieldOf("name", "").forGetter(SpellPage::name),
            Codec.STRING.optionalFieldOf("form", "").forGetter(SpellPage::form),
            Codec.STRING.listOf(0, MAX_HEARTS).optionalFieldOf("hearts", List.of()).forGetter(SpellPage::hearts),
            Codec.STRING.listOf(0, MAX_TOPS).optionalFieldOf("tops", List.of()).forGetter(SpellPage::tops),
            Codec.STRING.optionalFieldOf("overflow", "").forGetter(SpellPage::overflow),
            Codec.STRING.optionalFieldOf("seal", "").forGetter(SpellPage::seal)
    ).apply(instance, SpellPage::new));

    public static final StreamCodec<ByteBuf, SpellPage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_NAME * 4), SpellPage::name,
            ByteBufCodecs.STRING_UTF8, SpellPage::form,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_HEARTS)), SpellPage::hearts,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_TOPS)), SpellPage::tops,
            ByteBufCodecs.STRING_UTF8, SpellPage::overflow,
            ByteBufCodecs.STRING_UTF8, SpellPage::seal,
            SpellPage::new);

    public SpellPage {
        hearts = List.copyOf(hearts);
        tops = List.copyOf(tops);
    }

    public boolean isEmpty() {
        return form.isEmpty() && seal.isEmpty();
    }

    public boolean isSeal() {
        return !seal.isEmpty();
    }

    public SpellPage named(String newName) {
        return new SpellPage(newName, form, hearts, tops, overflow, seal);
    }

    public String canonical() {
        return form + "|" + String.join(",", hearts) + "|" + String.join(",", tops) + "|" + overflow + "|" + seal;
    }
}
