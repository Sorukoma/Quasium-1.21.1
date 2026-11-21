package net.sorukoma.quasium.selection.trade;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.selection.SelectionRegistry;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class CaravanLlamas {
    private CaravanLlamas() {
    }

    static void join(TraderLlama llama, long departsAt) {
        llama.setData(SelectionRegistry.CARAVAN_DEPARTURE, departsAt);
    }

    public static boolean carriesHookahs(Entity entity) {
        return entity instanceof TraderLlama && entity.hasData(SelectionRegistry.CARAVAN_DEPARTURE);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof TraderLlama llama) || llama.level().isClientSide) return;
        if (!llama.hasData(SelectionRegistry.CARAVAN_DEPARTURE) || keptBack(llama)) return;
        if (llama.level().getGameTime() < llama.getData(SelectionRegistry.CARAVAN_DEPARTURE)) return;
        llama.dropLeash(true, false);
        llama.discard();
    }

    private static boolean keptBack(TraderLlama llama) {
        if (llama.getLeashHolder() instanceof SmokeCaravanTrader caravan) return caravan.isAlive();
        return llama.isLeashed() || tiedToAFence(llama.getLeashData()) || llama.isTamed() || llama.isVehicle();
    }

    private static boolean tiedToAFence(@Nullable Leashable.LeashData leash) {
        return leash != null && leash.delayedLeashInfo != null && leash.delayedLeashInfo.right().isPresent();
    }
}
