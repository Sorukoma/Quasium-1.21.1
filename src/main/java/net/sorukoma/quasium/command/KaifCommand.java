package net.sorukoma.quasium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
// import com.mojang.brigadier.exceptions.CommandSyntaxException; // Удален неиспользуемый импорт
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.data.PlayerKaifData;

import java.util.Collection;

public class KaifCommand {

    public KaifCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kaif")
                .requires(source -> source.hasPermission(2)) // Требуется уровень разрешения 2 (модератор)
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, PlayerKaifData.MAX_KAIF))
                                        .executes(context -> setKaif(context, EntityArgument.getPlayers(context, "target"), IntegerArgumentType.getInteger(context, "value")))
                                )
                        )
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(context -> addKaif(context, EntityArgument.getPlayers(context, "target"), IntegerArgumentType.getInteger(context, "value")))
                                )
                        )
                )
                .then(Commands.literal("subtract")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(context -> subtractKaif(context, EntityArgument.getPlayers(context, "target"), IntegerArgumentType.getInteger(context, "value")))
                                )
                        )
                )
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getKaif(context, EntityArgument.getPlayer(context, "target")))
                        )
                )
        );
    }

    private static int setKaif(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, int value) { // Удален throws CommandSyntaxException
        for (ServerPlayer player : targets) {
            PlayerKaifData.setKaifValue(player, value);
            player.sendSystemMessage(Component.literal("Ваш уровень кайфа установлен на " + value));
            context.getSource().sendSuccess(() -> Component.literal("Уровень кайфа игрока " + player.getName().getString() + " установлен на " + value), true);
        }
        return 1;
    }

    private static int addKaif(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, int value) { // Удален throws CommandSyntaxException
        for (ServerPlayer player : targets) {
            // int currentKaif = PlayerKaifData.getKaifValue(player); // Удалена неиспользуемая переменная
            PlayerKaifData.addKaif(player, value);
            player.sendSystemMessage(Component.literal("Вам добавлено " + value + " кайфа. Текущий уровень: " + PlayerKaifData.getKaifValue(player)));
            context.getSource().sendSuccess(() -> Component.literal("Игроку " + player.getName().getString() + " добавлено " + value + " кайфа. Текущий уровень: " + PlayerKaifData.getKaifValue(player)), true);
        }
        return 1;
    }

    private static int subtractKaif(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, int value) { // Удален throws CommandSyntaxException
        for (ServerPlayer player : targets) {
            // int currentKaif = PlayerKaifData.getKaifValue(player); // Удалена неиспользуемая переменная
            PlayerKaifData.subtractKaif(player, value);
            player.sendSystemMessage(Component.literal("У вас отнято " + value + " кайфа. Текущий уровень: " + PlayerKaifData.getKaifValue(player)));
            context.getSource().sendSuccess(() -> Component.literal("У игрока " + player.getName().getString() + " отнято " + value + " кайфа. Текущий уровень: " + PlayerKaifData.getKaifValue(player)), true);
        }
        return 1;
    }

    private static int getKaif(CommandContext<CommandSourceStack> context, ServerPlayer target) { // Удален throws CommandSyntaxException
        int kaifValue = PlayerKaifData.getKaifValue(target);
        context.getSource().sendSuccess(() -> Component.literal("Уровень кайфа игрока " + target.getName().getString() + ": " + kaifValue), false);
        return kaifValue;
    }
}
