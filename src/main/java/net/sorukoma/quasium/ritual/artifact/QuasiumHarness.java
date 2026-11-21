package net.sorukoma.quasium.ritual.artifact;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.SmokeSource;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.SpellForm;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualRegistry;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public final class QuasiumHarness {
    private QuasiumHarness() {
    }

    public static ItemStack backpack(LivingEntity wearer) {
        return CuriosApi.getCuriosInventory(wearer).flatMap(curios -> curios.findFirstCurio(ModItems.BACKPACK_HOOKAH.get()))
                .map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    public static boolean worn(LivingEntity wearer) {
        return backpack(wearer).has(RitualRegistry.QUASIUM_HARNESS.get());
    }

    public static int lungs(ServerPlayer player, SmokeSource source, int capacity) {
        if (!pullsThroughHarness(player, source)) return capacity;
        return capacity * RitualConfig.HARNESS_LUNG_PCT.getAsInt() / 100;
    }

    public static int cooldown(ServerPlayer player, SmokeSource source, Spell spell) {
        int ticks = spell.cooldownTicks();
        if (spell.form() != SpellForm.RING || !pullsThroughHarness(player, source)) return ticks;
        return ticks * RitualConfig.HARNESS_RING_COOLDOWN_PCT.getAsInt() / 100;
    }

    private static boolean pullsThroughHarness(ServerPlayer player, SmokeSource source) {
        return source == BackpackHookahState.getState(player.getUUID()) && worn(player);
    }
}
