package net.sorukoma.quasium.magic.page.form;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.sorukoma.quasium.QuasiumMod;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class VeilSight {
    private VeilSight() {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && SmokeVeilEntity.hides(event.getEntity(), target)) event.setCanceled(true);
    }
}
