package net.sorukoma.quasium.golem.program;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum BlockKind implements StringRepresentable {
    GO("go", BlockGroup.ACTIONS, 0, Number.SECONDS),
    BREAK("break", BlockGroup.ACTIONS, 0, Number.NONE),
    TAKE("take", BlockGroup.ACTIONS, 0, Number.COUNT),
    PUT("put", BlockGroup.ACTIONS, 0, Number.COUNT),
    WAIT("wait", BlockGroup.ACTIONS, 0, Number.SECONDS),
    FOR_EACH("for_each", BlockGroup.ACTIONS, 1, Number.NONE),
    REPEAT("repeat", BlockGroup.LOOPS, 1, Number.TIMES),
    FOREVER("forever", BlockGroup.LOOPS, 1, Number.NONE),
    WHILE("while", BlockGroup.LOOPS, 1, Number.NONE),
    IF("if", BlockGroup.CONDITIONS, 1, Number.NONE),
    IF_ELSE("if_else", BlockGroup.CONDITIONS, 2, Number.NONE),
    WAIT_SIGNAL("wait_signal", BlockGroup.REDSTONE, 0, Number.NONE),
    PRESS("press", BlockGroup.REDSTONE, 0, Number.NONE);

    public static final StringRepresentable.EnumCodec<BlockKind> CODEC = StringRepresentable.fromEnum(BlockKind::values);
    public static final StreamCodec<ByteBuf, BlockKind> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(BlockKind::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), BlockKind::ordinal);

    public enum Number {
        NONE(0, 0, 0),
        SECONDS(0, 60, 0),
        COUNT(0, 64, 0),
        TIMES(1, 64, 2);

        public final int min;
        public final int max;
        public final int start;

        Number(int min, int max, int start) {
            this.min = min;
            this.max = max;
            this.start = start;
        }
    }

    private final String id;
    private final BlockGroup group;
    private final int bodies;
    private final Number number;

    BlockKind(String id, BlockGroup group, int bodies, Number number) {
        this.id = id;
        this.group = group;
        this.bodies = bodies;
        this.number = number;
    }

    public BlockGroup group() {
        return group;
    }

    public int bodies() {
        return bodies;
    }

    public Number number() {
        return number;
    }

    public boolean usesA() {
        return this != WAIT && this != FOREVER && this != REPEAT && this != WHILE && this != IF && this != IF_ELSE;
    }

    public boolean usesB() {
        return this == FOR_EACH;
    }

    public boolean usesFilter() {
        return this == TAKE || this == PUT;
    }

    public boolean usesCheck() {
        return this == WHILE || this == IF || this == IF_ELSE;
    }

    public String key() {
        return "quasium.program.block." + id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
