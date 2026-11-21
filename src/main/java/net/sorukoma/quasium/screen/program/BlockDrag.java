package net.sorukoma.quasium.screen.program;

import net.minecraft.client.gui.Font;
import net.sorukoma.quasium.golem.program.BlockKind;
import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramPath;
import org.jetbrains.annotations.Nullable;

public final class BlockDrag {
    private static final double START = 3.0;

    private final ProgramBlock block;
    @Nullable
    private final ProgramPath from;
    private final BlockLayout.Result ghost;
    private final int grabX;
    private final int grabY;
    private final double pressX;
    private final double pressY;
    private double mouseX;
    private double mouseY;
    private boolean started;

    private BlockDrag(ProgramBlock block, @Nullable ProgramPath from, Font font, int blockX, int blockY, double mouseX, double mouseY) {
        this.block = block;
        this.from = from;
        ghost = BlockLayout.single(block, font, false);
        grabX = (int) Math.floor(mouseX) - blockX;
        grabY = (int) Math.floor(mouseY) - blockY;
        pressX = this.mouseX = mouseX;
        pressY = this.mouseY = mouseY;
    }

    public static BlockDrag fromPalette(BlockKind kind, Font font, int blockX, int blockY, double mouseX, double mouseY) {
        return new BlockDrag(ProgramBlock.of(kind), null, font, blockX, blockY, mouseX, mouseY);
    }

    public static BlockDrag fromProgram(Placed placed, Font font, int blockX, int blockY, double mouseX, double mouseY) {
        return new BlockDrag(placed.block(), placed.path(), font, blockX, blockY, mouseX, mouseY);
    }

    public void moveTo(double x, double y) {
        mouseX = x;
        mouseY = y;
        if (!started && Math.hypot(x - pressX, y - pressY) >= START) started = true;
    }

    public boolean started() {
        return started;
    }

    public ProgramBlock block() {
        return block;
    }

    @Nullable
    public ProgramPath from() {
        return from;
    }

    @Nullable
    public ProgramPath lifted() {
        return started ? from : null;
    }

    public BlockLayout.Result ghost() {
        return ghost;
    }

    public double mouseX() {
        return mouseX;
    }

    public double mouseY() {
        return mouseY;
    }

    public int ghostX() {
        return (int) Math.floor(mouseX) - grabX;
    }

    public int ghostY() {
        return (int) Math.floor(mouseY) - grabY;
    }
}
