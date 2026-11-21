package net.sorukoma.quasium.magic.page.form;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.magic.page.SpellPage;
import net.sorukoma.quasium.magic.page.SpellTetradItem;
import net.sorukoma.quasium.magic.page.TetradContents;
import net.sorukoma.quasium.mind.research.Research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class PageSeals {
    private static final int CHECK_EVERY_TICKS = 20;
    private static final Map<UUID, Set<ResourceLocation>> DELIVERED = new HashMap<>();

    private PageSeals() {
    }

    public static Set<ResourceLocation> knownHidden(ServerPlayer player) {
        Set<ResourceLocation> hidden = new LinkedHashSet<>();
        for (ResourceLocation id : Research.knownSpells(player)) {
            Spell spell = Spells.registered(id);
            if (spell != null && spell.hidden()) hidden.add(id);
        }
        return hidden;
    }

    public static Set<ResourceLocation> deliver(ServerPlayer player, Collection<ResourceLocation> seals) {
        Set<ResourceLocation> placed = new LinkedHashSet<>();
        if (seals.isEmpty()) return placed;
        Stream.concat(player.getInventory().items.stream(), player.getInventory().offhand.stream())
                .filter(stack -> stack.getItem() instanceof SpellTetradItem)
                .forEach(tetrad -> placed.addAll(fill(tetrad, seals)));
        placed.forEach(seal -> player.displayClientMessage(Component.translatable("quasium.page.seal_added", Spell.displayName(seal)), false));
        return placed;
    }

    public static List<ResourceLocation> fill(ItemStack tetrad, Collection<ResourceLocation> seals) {
        TetradContents contents = SpellTetradItem.contents(tetrad);
        List<ResourceLocation> placed = new ArrayList<>();
        for (ResourceLocation seal : seals) {
            if (contents.pages().stream().anyMatch(page -> page.seal().equals(seal.toString()))) continue;
            int free = firstFree(contents);
            if (free < 0) break;
            contents = contents.with(free, ClassicPages.seal(seal));
            placed.add(seal);
        }
        if (!placed.isEmpty()) SpellTetradItem.store(tetrad, contents);
        return placed;
    }

    private static int firstFree(TetradContents contents) {
        for (int i = 0; i < contents.size(); i++) {
            if (contents.page(i).isEmpty()) return i;
        }
        return -1;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_EVERY_TICKS != 0) return;
        Set<ResourceLocation> known = knownHidden(player);
        Set<ResourceLocation> delivered = DELIVERED.get(player.getUUID());
        if (known.equals(delivered)) return;
        Set<ResourceLocation> fresh = new LinkedHashSet<>(known);
        if (delivered != null) fresh.removeAll(delivered);
        DELIVERED.put(player.getUUID(), known);
        deliver(player, fresh);
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getCrafting().getItem() instanceof SpellTetradItem) {
            fill(event.getCrafting(), knownHidden(player));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DELIVERED.remove(event.getEntity().getUUID());
    }
}
