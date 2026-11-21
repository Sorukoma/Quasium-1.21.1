package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record FocusEssence(String essence, int ml) {
    public static final int MAX_SLOTS = 3;

    public static final Codec<FocusEssence> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("essence").forGetter(FocusEssence::essence),
            Codec.INT.fieldOf("ml").forGetter(FocusEssence::ml)
    ).apply(instance, FocusEssence::new));

    public static final StreamCodec<ByteBuf, FocusEssence> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FocusEssence::essence,
            ByteBufCodecs.VAR_INT, FocusEssence::ml,
            FocusEssence::new);

    public static final Codec<List<FocusEssence>> LIST_CODEC = CODEC.listOf();
    public static final StreamCodec<ByteBuf, List<FocusEssence>> LIST_STREAM_CODEC =
            STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SLOTS));

    public boolean isEmpty() {
        return essence.isBlank() || ml <= 0;
    }

    public static List<String> activeEssences(List<FocusEssence> focus) {
        List<String> essences = new ArrayList<>();
        for (FocusEssence entry : focus) {
            if (!entry.isEmpty()) essences.add(entry.essence());
        }
        return essences;
    }

    public static List<FocusEssence> drained(List<FocusEssence> focus, int ml) {
        List<FocusEssence> result = new ArrayList<>(focus.size());
        for (FocusEssence entry : focus) {
            result.add(entry.isEmpty() ? entry : new FocusEssence(entry.essence(), Math.max(0, entry.ml() - ml)));
        }
        return result;
    }
}
