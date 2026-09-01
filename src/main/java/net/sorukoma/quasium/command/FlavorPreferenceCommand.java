package net.sorukoma.quasium.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.data.PlayerFlavorPreferenceData;

import java.util.Map;

public final class FlavorPreferenceCommand {
    private FlavorPreferenceCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quasium")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("flavors")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> showPreferences(context.getSource(), EntityArgument.getPlayer(context, "target")))
                        )
                        .executes(context -> {
                            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                                context.getSource().sendFailure(Component.translatable("quasium.command.flavors.usage"));
                                return 0;
                            }
                            return showPreferences(context.getSource(), player);
                        })
                )
        );
    }

    private static int showPreferences(CommandSourceStack source, ServerPlayer player) {
        Map<String, Integer> preferences = PlayerFlavorPreferenceData.getAllPreferences(player);
        source.sendSuccess(() -> Component.translatable("quasium.command.flavors.header", player.getDisplayName()), false);
        for (Map.Entry<String, Integer> entry : preferences.entrySet()) {
            Component status = Component.translatable(entry.getValue() > 0
                    ? "quasium.command.flavors.liked"
                    : "quasium.command.flavors.disliked");
            source.sendSuccess(() -> Component.translatable("quasium.command.flavors.entry",
                    Component.translatable("quasium.flavor." + entry.getKey()),
                    status,
                    entry.getValue()), false);
        }
        return preferences.size();
    }
}
