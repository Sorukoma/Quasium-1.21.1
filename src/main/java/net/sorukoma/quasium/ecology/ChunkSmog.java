package net.sorukoma.quasium.ecology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ChunkSmog(int level, int fullMinutes, int cleanMinutes) {
    public static final ChunkSmog CLEAR = new ChunkSmog(0, 0, 0);
    public static final Codec<ChunkSmog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("level", 0).forGetter(ChunkSmog::level),
            Codec.INT.optionalFieldOf("full_minutes", 0).forGetter(ChunkSmog::fullMinutes),
            Codec.INT.optionalFieldOf("clean_minutes", 0).forGetter(ChunkSmog::cleanMinutes)
    ).apply(instance, ChunkSmog::new));

    public ChunkSmog withLevel(int newLevel) {
        return new ChunkSmog(Math.max(0, Math.min(SmogMath.MAX_LEVEL, newLevel)), fullMinutes, newLevel > level ? 0 : cleanMinutes);
    }

    public boolean isClear() {
        return equals(CLEAR);
    }
}
