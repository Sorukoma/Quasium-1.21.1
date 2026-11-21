package net.sorukoma.quasium.screen.program;

import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramPath;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record Placed(ProgramPath path, ProgramBlock block, int x, int y, int width, int height, List<Part> parts, List<Mouth> mouths) {
    public record Mouth(int top, int bottom) {
    }

    public boolean onPlate(int pointX, int pointY) {
        if (pointX < x || pointY < y || pointY >= y + height) return false;
        if (pointX < x + BlockLayout.ARM) return true;
        if (pointX >= x + width) return false;
        for (Mouth mouth : mouths) {
            if (pointY >= mouth.top && pointY < mouth.bottom) return false;
        }
        return true;
    }

    @Nullable
    public Part partAt(int pointX, int pointY) {
        for (int index = parts.size() - 1; index >= 0; index--) {
            Part part = parts.get(index);
            if (part.kind().clickable() && part.contains(pointX, pointY)) return part;
        }
        return null;
    }
}
