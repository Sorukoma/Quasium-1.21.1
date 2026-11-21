package net.sorukoma.quasium.golem.program;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record ProgramBlock(BlockKind kind, Optional<MarkedPlace> a, Optional<MarkedPlace> b, ItemStack filter, int number, Check check,
                           List<ProgramBlock> body, List<ProgramBlock> otherwise) {
    public static final int CORNER_A = 0;
    public static final int CORNER_B = 1;
    public static final Codec<ProgramBlock> CODEC = Codec.recursive("ProgramBlock", self -> RecordCodecBuilder.create(instance -> instance.group(
            BlockKind.CODEC.fieldOf("kind").forGetter(ProgramBlock::kind),
            MarkedPlace.CODEC.optionalFieldOf("a").forGetter(ProgramBlock::a),
            MarkedPlace.CODEC.optionalFieldOf("b").forGetter(ProgramBlock::b),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("filter", ItemStack.EMPTY).forGetter(ProgramBlock::filter),
            Codec.INT.optionalFieldOf("number", 0).forGetter(ProgramBlock::number),
            Check.CODEC.optionalFieldOf("check", Check.LOAD_FULL).forGetter(ProgramBlock::check),
            self.listOf().optionalFieldOf("body", List.of()).forGetter(ProgramBlock::body),
            self.listOf().optionalFieldOf("otherwise", List.of()).forGetter(ProgramBlock::otherwise)
    ).apply(instance, ProgramBlock::new)));

    public ProgramBlock {
        number = Mth.clamp(number, kind.number().min, kind.number().max);
        filter = filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1);
        body = kind.bodies() > 0 ? List.copyOf(body) : List.of();
        otherwise = kind.bodies() > 1 ? List.copyOf(otherwise) : List.of();
    }

    public static ProgramBlock of(BlockKind kind) {
        return new ProgramBlock(kind, Optional.empty(), Optional.empty(), ItemStack.EMPTY, kind.number().start, Check.LOAD_FULL, List.of(), List.of());
    }

    public boolean usesA() {
        return kind.usesA() || kind.usesCheck() && check.usesA();
    }

    public boolean usesB() {
        return kind.usesB();
    }

    public boolean usesFilter() {
        return kind.usesFilter() || kind.usesCheck() && check.usesFilter();
    }

    public boolean uses(int corner) {
        return corner == CORNER_A ? usesA() : corner == CORNER_B && usesB();
    }

    public Optional<MarkedPlace> place(int corner) {
        return corner == CORNER_A ? a : b;
    }

    public List<ProgramBlock> bodyOf(int which) {
        return which == 0 ? body : otherwise;
    }

    public int size() {
        int size = 1;
        for (ProgramBlock inner : body) size += inner.size();
        for (ProgramBlock inner : otherwise) size += inner.size();
        return size;
    }

    public int nesting() {
        if (kind.bodies() == 0) return 0;
        int deepest = 0;
        for (ProgramBlock inner : body) deepest = Math.max(deepest, inner.nesting());
        for (ProgramBlock inner : otherwise) deepest = Math.max(deepest, inner.nesting());
        return deepest + 1;
    }

    public ProgramBlock withPlace(int corner, Optional<MarkedPlace> place) {
        return corner == CORNER_A ? new ProgramBlock(kind, place, b, filter, number, check, body, otherwise)
                : new ProgramBlock(kind, a, place, filter, number, check, body, otherwise);
    }

    public ProgramBlock withFilter(ItemStack filter) {
        return new ProgramBlock(kind, a, b, filter, number, check, body, otherwise);
    }

    public ProgramBlock withNumber(int number) {
        return new ProgramBlock(kind, a, b, filter, number, check, body, otherwise);
    }

    public ProgramBlock withCheck(Check check) {
        return new ProgramBlock(kind, a, b, filter, number, check, body, otherwise);
    }

    public ProgramBlock withBody(int which, List<ProgramBlock> blocks) {
        return which == 0 ? new ProgramBlock(kind, a, b, filter, number, check, blocks, otherwise)
                : new ProgramBlock(kind, a, b, filter, number, check, body, blocks);
    }
}
