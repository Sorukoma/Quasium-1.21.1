package net.sorukoma.quasium.golem.program;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.List;

public record ProgramEdit(Op op, ProgramPath at, ProgramPath to, int value) {
    private static final ProgramPath NOWHERE = new ProgramPath(List.of());

    public enum Op {
        ADD, MOVE, REMOVE, NUMBER, CHECK, PLACE, FILTER, SAVE_CARD, LOAD_CARD;

        static final StreamCodec<ByteBuf, Op> STREAM_CODEC = ByteBufCodecs.idMapper(
                ByIdMap.continuous(Op::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Op::ordinal);
    }

    public static final StreamCodec<ByteBuf, ProgramEdit> STREAM_CODEC = StreamCodec.composite(
            Op.STREAM_CODEC, ProgramEdit::op, ProgramPath.STREAM_CODEC, ProgramEdit::at,
            ProgramPath.STREAM_CODEC, ProgramEdit::to, ByteBufCodecs.VAR_INT, ProgramEdit::value, ProgramEdit::new);

    public static ProgramEdit add(ProgramPath spot, BlockKind kind) {
        return new ProgramEdit(Op.ADD, spot, NOWHERE, kind.ordinal());
    }

    public static ProgramEdit move(ProgramPath from, ProgramPath spot) {
        return new ProgramEdit(Op.MOVE, from, spot, 0);
    }

    public static ProgramEdit remove(ProgramPath path) {
        return new ProgramEdit(Op.REMOVE, path, NOWHERE, 0);
    }

    public static ProgramEdit number(ProgramPath path, int number) {
        return new ProgramEdit(Op.NUMBER, path, NOWHERE, number);
    }

    public static ProgramEdit check(ProgramPath path, Check check) {
        return new ProgramEdit(Op.CHECK, path, NOWHERE, check.ordinal());
    }

    public static ProgramEdit place(ProgramPath path, int corner) {
        return new ProgramEdit(Op.PLACE, path, NOWHERE, corner);
    }

    public static ProgramEdit filter(ProgramPath path) {
        return new ProgramEdit(Op.FILTER, path, NOWHERE, 0);
    }

    public static ProgramEdit saveCard() {
        return new ProgramEdit(Op.SAVE_CARD, NOWHERE, NOWHERE, 0);
    }

    public static ProgramEdit loadCard() {
        return new ProgramEdit(Op.LOAD_CARD, NOWHERE, NOWHERE, 0);
    }
}
