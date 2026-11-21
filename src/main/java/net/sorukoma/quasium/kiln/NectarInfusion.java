package net.sorukoma.quasium.kiln;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class NectarInfusion {
    private NectarInfusion() {
    }

    @SubscribeEvent
    public static void onUseHoe(PlayerInteractEvent.RightClickBlock event) {
        ItemStack hoe = event.getItemStack();
        Level level = event.getLevel();
        if (!hoe.is(ItemTags.HOES) || NoteInfusion.of(hoe) != NoteType.NECTAR || !event.getEntity().mayBuild()
                || !grown(level.getBlockState(event.getPos()))) return;
        if (level instanceof ServerLevel serverLevel) {
            for (BlockPos pos : BlockPos.betweenClosed(event.getPos().offset(-1, 0, -1), event.getPos().offset(1, 0, 1))) {
                harvest(serverLevel, pos.immutable(), event.getEntity(), hoe);
            }
            hoe.hurtAndBreak(1, event.getEntity(), LivingEntity.getSlotForHand(event.getHand()));
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }

    private static boolean grown(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private static void harvest(ServerLevel level, BlockPos pos, Player player, ItemStack hoe) {
        BlockState state = level.getBlockState(pos);
        if (!grown(state)) return;
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, hoe);
        boolean replanted = takeSeed(drops, state);
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        level.setBlock(pos, replanted ? ((CropBlock) state.getBlock()).getStateForAge(0) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        drops.forEach(drop -> Block.popResource(level, pos, drop));
    }

    private static boolean takeSeed(List<ItemStack> drops, BlockState crop) {
        for (ItemStack drop : drops) {
            if (drop.is(crop.getBlock().asItem())) {
                drop.shrink(1);
                return true;
            }
        }
        return false;
    }
}
