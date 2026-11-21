package net.sorukoma.quasium.magic.page.form;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.FocusUpgrade;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.SpellCaster;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class PageEcho {
    private static final List<Pending> PENDING = new ArrayList<>();

    private record Pending(ServerPlayer caster, ResourceLocation spellId, Spell spell, float power,
                           @Nullable FocusUpgrade upgrade, long dueAt) {
    }

    private PageEcho() {
    }

    public static void schedule(ServerPlayer caster, ResourceLocation spellId, Spell spell, float power, @Nullable FocusUpgrade upgrade) {
        float echoPower = power * FormConfig.ECHO_POWER_PCT.getAsInt() / 100f;
        long dueAt = caster.level().getGameTime() + FormConfig.ECHO_DELAY_TICKS.getAsInt();
        PENDING.add(new Pending(caster, spellId, spell, echoPower, upgrade, dueAt));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        List<Pending> due = new ArrayList<>();
        Iterator<Pending> waiting = PENDING.iterator();
        while (waiting.hasNext()) {
            Pending pending = waiting.next();
            ServerPlayer caster = pending.caster();
            if (caster.isRemoved() || !caster.isAlive()) {
                waiting.remove();
            } else if (caster.level().getGameTime() >= pending.dueAt()) {
                waiting.remove();
                due.add(pending);
            }
        }
        due.forEach(echo -> SpellCaster.exhale(echo.caster(), echo.spellId(), echo.spell(), echo.power(), echo.upgrade()));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }
}
