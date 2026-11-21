package net.sorukoma.quasium.screen.program;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.golem.GolemBindings;
import net.sorukoma.quasium.golem.program.BlockKind;
import net.sorukoma.quasium.golem.program.Check;
import net.sorukoma.quasium.golem.program.MarkedPlace;
import net.sorukoma.quasium.golem.program.ProgramBlock;
import net.sorukoma.quasium.golem.program.ProgramUnlocks;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.screen.ProgramTableMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProgramTips {
    private static final int HINT = 0x8FA3B8;
    private ProgramTips() {
    }

    public static List<Component> palette(BlockKind kind, Player player) {
        List<Component> lines = describe(kind);
        if (ProgramUnlocks.mayUse(player, kind)) {
            lines.add(hint("quasium.program.ui.drag_palette"));
            return lines;
        }
        ResourceLocation node = kind.group().research();
        lines.add(Component.translatable("quasium.program.ui.locked", Research.title(String.valueOf(node))).withStyle(ChatFormatting.RED));
        return lines;
    }

    public static List<Component> block(ProgramBlock block, boolean running) {
        List<Component> lines = describe(block.kind());
        if (running) lines.add(Component.translatable("quasium.program.ui.running").withStyle(ChatFormatting.GOLD));
        lines.add(hint("quasium.program.ui.drag_program"));
        return lines;
    }

    public static List<Component> part(ProgramBlock block, Part part, ProgramTableMenu menu, Player player) {
        return switch (part.kind()) {
            case SOCKET -> socket(block, part, menu, player.level());
            case FILTER -> filter(block.filter());
            case NUMBER -> List.of(Component.literal(part.text()), hint("quasium.program.ui.number"));
            case CHECK -> check(block.check(), player);
            default -> List.of();
        };
    }

    private static List<Component> describe(BlockKind kind) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(kind.key()));
        lines.add(Component.translatable(kind.key() + ".desc").withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static Component hint(String key) {
        return Component.translatable(key).withColor(HINT);
    }

    private static List<Component> socket(ProgramBlock block, Part part, ProgramTableMenu menu, Level level) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(part.corner() == ProgramBlock.CORNER_A ? "quasium.program.ui.place_a" : "quasium.program.ui.place_b"));
        Optional<MarkedPlace> place = block.place(part.corner());
        if (place.isEmpty()) {
            if (part.width() > Part.HEIGHT) lines.add(Component.translatable("quasium.program.ui.here_desc").withStyle(ChatFormatting.GRAY));
            lines.add(hint("quasium.program.ui.place_set"));
            return lines;
        }
        BlockPos pos = place.get().where().pos();
        lines.add(Component.translatable("item.quasium.place_marker.place", pos.getX(), pos.getY(), pos.getZ(),
                Component.translatable("quasium.place_marker.face." + place.get().face().getSerializedName())).withStyle(ChatFormatting.AQUA));
        if (menu.far(level, place.get())) {
            lines.add(Component.translatable("quasium.program.ui.far", GolemBindings.markRadius()).withStyle(ChatFormatting.RED));
        }
        lines.add(hint("quasium.program.ui.place_set"));
        lines.add(hint("quasium.program.ui.clear"));
        return lines;
    }

    private static List<Component> filter(ItemStack filter) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("quasium.program.ui.filter"));
        if (!filter.isEmpty()) lines.add(filter.getHoverName().copy().withStyle(ChatFormatting.AQUA));
        lines.add(hint("quasium.program.ui.filter_set"));
        if (!filter.isEmpty()) lines.add(hint("quasium.program.ui.clear"));
        return lines;
    }

    private static List<Component> check(Check check, Player player) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(check.key()));
        lines.add(Component.translatable(check.key() + ".desc").withStyle(ChatFormatting.GRAY));
        lines.add(hint("quasium.program.ui.check"));
        for (Check other : Check.values()) {
            if (ProgramUnlocks.mayUse(player, other)) continue;
            lines.add(Component.translatable("quasium.program.ui.check_locked", Research.title(String.valueOf(other.group().research())))
                    .withStyle(ChatFormatting.RED));
            break;
        }
        return lines;
    }
}
