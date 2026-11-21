package net.sorukoma.quasium.magic.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.sorukoma.quasium.magic.NoteType;

import java.util.List;

public record EssenceNotes(List<NoteType> notes) {
    public static final Codec<EssenceNotes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NoteType.CODEC.listOf().fieldOf("notes").forGetter(EssenceNotes::notes)
    ).apply(instance, EssenceNotes::new));
}
