package net.sorukoma.quasium.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.kiln.NoteInfusion;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualRegistry;

public final class GearDurability {
    private GearDurability() {
    }

    public static int bonusPct(ItemStack stack) {
        int pct = 0;
        if (NoteInfusion.of(stack) == NoteType.RESIN) pct += ConfigArgs.synced(Config.INFUSION_RESIN_DURABILITY_PCT);
        if (stack.has(RitualRegistry.SMOKE_TEMPERED.get())) pct += ConfigArgs.synced(RitualConfig.SMOKE_TEMPER_DURABILITY_PCT);
        return pct;
    }

    public static void refresh(ItemStack stack) {
        int base = stack.getPrototype().getOrDefault(DataComponents.MAX_DAMAGE, stack.getMaxDamage());
        int durability = base + base * bonusPct(stack) / 100;
        if (durability <= 0 || durability == stack.getMaxDamage()) return;
        stack.set(DataComponents.MAX_DAMAGE, durability);
        stack.setDamageValue(Math.min(stack.getDamageValue(), durability - 1));
    }
}
