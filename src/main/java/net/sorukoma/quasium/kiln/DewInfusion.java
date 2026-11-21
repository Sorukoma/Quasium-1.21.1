package net.sorukoma.quasium.kiln;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class DewInfusion {
    private DewInfusion() {
    }

    private static boolean dewShears(ItemStack stack) {
        return stack.is(Tags.Items.TOOLS_SHEAR) && NoteInfusion.of(stack) == NoteType.DEW;
    }

    private static int extra(ItemStack drop) {
        return Math.min(Config.INFUSION_DEW_EXTRA_DROPS.getAsInt(), drop.getMaxStackSize() - drop.getCount());
    }

    @SubscribeEvent
    public static void onShearSheep(PlayerInteractEvent.EntityInteract event) {
        ItemStack shears = event.getItemStack();
        Level level = event.getLevel();
        if (level.isClientSide || !(event.getTarget() instanceof Sheep sheep) || !dewShears(shears)) return;
        Player player = event.getEntity();
        BlockPos pos = sheep.blockPosition();
        if (!sheep.isShearable(player, shears, level, pos)) return;
        List<ItemStack> wool = sheep.onSheared(player, shears, level, pos);
        if (!wool.isEmpty()) wool.getFirst().grow(extra(wool.getFirst()));
        wool.forEach(drop -> sheep.spawnShearedDrop(level, pos, drop));
        sheep.gameEvent(GameEvent.SHEAR, player);
        shears.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onShearBlock(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!dewShears(event.getTool()) || !(state.is(BlockTags.LEAVES) || state.is(Blocks.VINE))) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(state.getBlock().asItem())) {
                stack.grow(extra(stack));
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        ItemStack rod = player.getMainHandItem().canPerformAction(ItemAbilities.FISHING_ROD_CAST)
                ? player.getMainHandItem() : player.getOffhandItem();
        if (NoteInfusion.of(rod) != NoteType.DEW || event.getDrops().isEmpty()) return;
        ItemStack caught = event.getDrops().getFirst();
        int extra = extra(caught);
        if (extra <= 0) return;
        FishingHook hook = event.getHookEntity();
        ItemEntity bonus = new ItemEntity(hook.level(), hook.getX(), hook.getY(), hook.getZ(), caught.copyWithCount(extra));
        double dx = player.getX() - hook.getX();
        double dy = player.getY() - hook.getY();
        double dz = player.getZ() - hook.getZ();
        bonus.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
        hook.level().addFreshEntity(bonus);
    }
}
