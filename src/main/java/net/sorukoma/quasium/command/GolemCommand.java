package net.sorukoma.quasium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.sorukoma.quasium.golem.GolemMood;
import net.sorukoma.quasium.golem.WorkerGolem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class GolemCommand {
    private GolemCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("golem")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("mood")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> run(context, null))
                                .then(Commands.literal("meter")
                                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                                .executes(context -> run(context, golem -> golem.setMood(golem.mood().state(),
                                                        IntegerArgumentType.getInteger(context, "percent"))))))
                                .then(Commands.literal("state")
                                        .then(Commands.argument("state", IntegerArgumentType.integer(GolemMood.BEST, GolemMood.WORST))
                                                .executes(context -> run(context, golem -> golem.setMood(
                                                        IntegerArgumentType.getInteger(context, "state"), golem.mood().meter())))))
                                .then(Commands.literal("reset")
                                        .executes(context -> run(context, golem -> golem.setMood(GolemMood.BEST, GolemMood.FULL)))))));
    }

    private static int run(CommandContext<CommandSourceStack> context, @Nullable Consumer<WorkerGolem> change) throws CommandSyntaxException {
        List<WorkerGolem> golems = EntityArgument.getOptionalEntities(context, "targets").stream()
                .filter(WorkerGolem.class::isInstance).map(WorkerGolem.class::cast).toList();
        if (golems.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("quasium.command.golem.none"));
            return 0;
        }
        for (WorkerGolem golem : golems) {
            if (change != null) change.accept(golem);
            context.getSource().sendSuccess(() -> describe(golem), change != null);
        }
        return golems.size();
    }

    private static Component describe(WorkerGolem golem) {
        GolemMood mood = golem.mood();
        return Component.translatable("quasium.command.golem.mood", golem.getDisplayName(), golem.blockPosition().toShortString(),
                WorkerGolem.moodName(mood.state()), Math.round(mood.meter()), mood.productivity(), mood.misses(), GolemMood.STREAK,
                mood.streak(), GolemMood.STREAK);
    }
}
