package net.sorukoma.quasium.screen.program;

import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.sorukoma.quasium.golem.program.BlockKind;
import net.sorukoma.quasium.golem.program.Program;
import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramPath;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockLayout {
    static final int ROW = 14;
    public static final int ARM = 8;
    private static final int ELSE_ROW = 12;
    private static final int GAP = 1;
    static final int MOUTH = 12;
    static final int PAD = 4;
    private static final int SPACE = 3;
    private static final int PILL_PAD = 3;
    private static final int SOCKET = 12;
    private static final int FOOT = 6;
    private static final int COMPACT_FOOT = 5;
    private static final int COMPACT_MOUTH = 3;
    private static final int MIN_PLAIN = 40;
    private static final int MIN_BRACKET = 64;
    private static final Pattern TOKEN = Pattern.compile("(?<![\\p{L}\\p{N}])[ABN](?![\\p{L}\\p{N}])");

    private final Font font;
    private final boolean compact;
    private final List<Placed> placed = new ArrayList<>();
    private final List<Spot> spots = new ArrayList<>();

    public record Result(List<Placed> placed, List<Spot> spots, int height) {
    }

    private record Segment(String text, char token) {
    }

    private BlockLayout(Font font, boolean compact) {
        this.font = font;
        this.compact = compact;
    }

    public static Result program(Program program, Font font) {
        BlockLayout layout = new BlockLayout(font, false);
        int bottom = layout.list(program.blocks(), null, 0, 0, 0, false);
        return new Result(List.copyOf(layout.placed), List.copyOf(layout.spots), bottom);
    }

    public static Result single(ProgramBlock block, Font font, boolean compact) {
        BlockLayout layout = new BlockLayout(font, compact);
        int bottom = layout.block(block, ProgramPath.top(0), 0, 0, false);
        return new Result(List.copyOf(layout.placed), List.of(), bottom);
    }

    private static String label(BlockKind kind) {
        return I18n.get((kind == BlockKind.IF_ELSE ? BlockKind.IF : kind).key());
    }

    private static ProgramPath at(@Nullable ProgramPath owner, int body, int index) {
        return owner == null ? ProgramPath.top(index) : owner.child(body, index);
    }

    private int list(List<ProgramBlock> blocks, @Nullable ProgramPath owner, int body, int x, int y, boolean here) {
        if (blocks.isEmpty()) {
            spots.add(new Spot(at(owner, body, 0), x, y, owner != null));
            return y + (compact ? COMPACT_MOUTH : MOUTH);
        }
        for (int index = 0; index < blocks.size(); index++) {
            spots.add(new Spot(at(owner, body, index), x, y, false));
            y = block(blocks.get(index), at(owner, body, index), x, y, here) + GAP;
        }
        spots.add(new Spot(at(owner, body, blocks.size()), x, y, false));
        return y;
    }

    private int block(ProgramBlock block, ProgramPath path, int x, int y, boolean here) {
        List<Part> parts = header(block, x, y, here);
        int right = parts.isEmpty() ? x : parts.getLast().x() + parts.getLast().width();
        int width = Math.max(right + PAD - x, block.kind().bodies() > 0 ? MIN_BRACKET : MIN_PLAIN);
        int index = placed.size();
        placed.add(null);
        List<Placed.Mouth> mouths = new ArrayList<>();
        boolean inside = here || block.kind() == BlockKind.FOR_EACH;
        int bottom = y + ROW;
        for (int body = 0; body < block.kind().bodies(); body++) {
            if (body == 1) {
                parts.add(new Part(Part.Kind.TEXT, x + PAD, bottom, font.width(I18n.get("quasium.program.ui.else")),
                        I18n.get("quasium.program.ui.else"), 0));
                bottom += ELSE_ROW;
            }
            int top = bottom;
            bottom = list(block.bodyOf(body), path, body, x + ARM, top, inside);
            mouths.add(new Placed.Mouth(top, bottom));
        }
        if (block.kind().bodies() > 0) bottom += compact ? COMPACT_FOOT : FOOT;
        placed.set(index, new Placed(path, block, x, y, width, bottom - y, parts, mouths));
        return bottom;
    }

    private List<Part> header(ProgramBlock block, int x, int y, boolean here) {
        Row row = new Row(x + PAD, y + 1, here);
        boolean inlineA = false;
        boolean inlineB = false;
        boolean inlineNumber = false;
        for (Segment segment : segments(label(block.kind()))) {
            switch (segment.token) {
                case 'A' -> inlineA = row.socket(block, ProgramBlock.CORNER_A);
                case 'B' -> inlineB = row.socket(block, ProgramBlock.CORNER_B);
                case 'N' -> inlineNumber = row.number(block, true);
                default -> row.text(Part.Kind.TEXT, segment.text);
            }
        }
        if (block.kind().usesA() && !inlineA) row.socket(block, ProgramBlock.CORNER_A);
        if (block.usesB() && !inlineB) row.socket(block, ProgramBlock.CORNER_B);
        if (block.kind().usesCheck()) row.check(block);
        if (block.usesFilter()) row.add(Part.Kind.FILTER, SOCKET, "", 0);
        if (block.kind().number() != BlockKind.Number.NONE && !inlineNumber) row.number(block, false);
        return row.parts;
    }

    private static List<Segment> segments(String label) {
        List<Segment> segments = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(label);
        int from = 0;
        while (matcher.find()) {
            text(segments, label.substring(from, matcher.start()));
            segments.add(new Segment("", label.charAt(matcher.start())));
            from = matcher.end();
        }
        text(segments, label.substring(from));
        return segments;
    }

    private static void text(List<Segment> segments, String text) {
        String trimmed = text.strip();
        if (!trimmed.isEmpty()) segments.add(new Segment(trimmed, '\0'));
    }

    private static String numberText(ProgramBlock block, boolean inline) {
        BlockKind.Number number = block.kind().number();
        if (number == BlockKind.Number.COUNT && block.number() == 0) return I18n.get("quasium.program.ui.all");
        if (inline) return Integer.toString(block.number());
        return I18n.get(switch (number) {
            case SECONDS -> "quasium.program.ui.seconds";
            case COUNT -> "quasium.program.ui.count";
            default -> "quasium.program.ui.times";
        }, block.number());
    }

    private final class Row {
        private final List<Part> parts = new ArrayList<>();
        private final int top;
        private final boolean here;
        private int cursor;

        private Row(int cursor, int top, boolean here) {
            this.cursor = cursor;
            this.top = top;
            this.here = here;
        }

        private void add(Part.Kind kind, int width, String text, int corner) {
            if (!parts.isEmpty() && parts.getLast().kind() != Part.Kind.CHECK) cursor += SPACE;
            parts.add(new Part(kind, cursor, top, width, text, corner));
            cursor += width;
        }

        private void text(Part.Kind kind, String text) {
            add(kind, font.width(text), text, 0);
        }

        private boolean socket(ProgramBlock block, int corner) {
            boolean thisBlock = corner == ProgramBlock.CORNER_A && here && block.place(corner).isEmpty();
            String text = thisBlock ? I18n.get("quasium.program.ui.here") : corner == ProgramBlock.CORNER_A ? "A" : "B";
            add(Part.Kind.SOCKET, thisBlock ? font.width(text) + 4 : SOCKET, text, corner);
            return true;
        }

        private boolean number(ProgramBlock block, boolean inline) {
            String text = numberText(block, inline);
            add(Part.Kind.NUMBER, Math.max(SOCKET, font.width(text) + 6), text, 0);
            return true;
        }

        private void check(ProgramBlock block) {
            add(Part.Kind.CHECK, 0, "", 0);
            int pill = parts.size() - 1;
            int start = parts.get(pill).x();
            cursor = start + PILL_PAD;
            boolean inlineA = false;
            for (Segment segment : segments(I18n.get(block.check().key()))) {
                if (segment.token == 'A') inlineA = socket(block, ProgramBlock.CORNER_A);
                else if (segment.token == '\0') text(Part.Kind.PILL_TEXT, segment.text);
            }
            if (block.check().usesA() && !inlineA) socket(block, ProgramBlock.CORNER_A);
            cursor += PILL_PAD;
            parts.set(pill, new Part(Part.Kind.CHECK, start, top, cursor - start, "", 0));
        }
    }
}
