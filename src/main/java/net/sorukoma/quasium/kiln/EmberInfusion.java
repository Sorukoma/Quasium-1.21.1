package net.sorukoma.quasium.kiln;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class EmberInfusion {
    private EmberInfusion() {
    }

    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon != null && NoteInfusion.of(weapon) == NoteType.EMBER) {
            event.getEntity().igniteForSeconds(Config.INFUSION_EMBER_FIRE_SECONDS.getAsInt());
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (NoteInfusion.of(event.getTool()) != NoteType.EMBER
                || event.getState().getDestroySpeed(event.getLevel(), event.getPos()) <= 0
                || event.getLevel().random.nextInt(100) >= Config.INFUSION_EMBER_DROP_PCT.getAsInt()) return;
        Block.popResource(event.getLevel(), event.getPos(), new ItemStack(KilnBlocks.EMBER_CHUNK.get()));
    }
}
