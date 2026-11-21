package net.sorukoma.quasium.smokelands.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.ritual.armor.Smokestep;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class AshSachetItem extends Item {
    public AshSachetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sachet = player.getItemInHand(hand);
        Optional<Item> waiting = longestCooldown(player);
        boolean dash = Smokestep.cooldownLeft(player) > waiting.map(item -> player.getCooldowns().getCooldownPercent(item, 0f)).orElse(0f);
        if (waiting.isEmpty() && !dash) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("quasium.ash_sachet.nothing"), true);
            return InteractionResultHolder.fail(sachet);
        }
        if (player instanceof ServerPlayer server) {
            Component wiped;
            if (dash) {
                Smokestep.resetCooldown(server);
                wiped = player.getItemBySlot(EquipmentSlot.FEET).getHoverName();
            } else {
                player.getCooldowns().removeCooldown(waiting.get());
                wiped = waiting.get().getDescription();
            }
            sachet.consume(1, player);
            level.playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);
            player.displayClientMessage(Component.translatable("quasium.ash_sachet.reset", wiped), true);
            SmokecraftTrigger.fire(server, "sachet_used");
        }
        return InteractionResultHolder.sidedSuccess(sachet, level.isClientSide);
    }

    public static Optional<Item> longestCooldown(Player player) {
        ItemCooldowns cooldowns = player.getCooldowns();
        return Stream.of(player.getInventory().items, player.getInventory().armor, player.getInventory().offhand)
                .flatMap(List::stream)
                .map(ItemStack::getItem)
                .filter(item -> !(item instanceof AshSachetItem) && cooldowns.isOnCooldown(item))
                .max(Comparator.comparingDouble(item -> cooldowns.getCooldownPercent(item, 0f)));
    }
}
