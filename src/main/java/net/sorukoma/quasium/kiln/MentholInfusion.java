package net.sorukoma.quasium.kiln;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;

import java.util.Set;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class MentholInfusion {
    private static final Set<Block> FROZEN = Set.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SNOW_BLOCK, Blocks.SNOW);

    private MentholInfusion() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack pickaxe = player.getMainHandItem();
        BlockState state = event.getState();
        if (player.isCreative() || !FROZEN.contains(state.getBlock()) || !pickaxe.is(ItemTags.PICKAXES)
                || NoteInfusion.of(pickaxe) != NoteType.MENTHOL || !(event.getLevel() instanceof Level level)) return;
        BlockPos pos = event.getPos();
        event.setCanceled(true);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
        Block.popResource(level, pos, state.is(Blocks.SNOW)
                ? new ItemStack(Items.SNOW, state.getValue(SnowLayerBlock.LAYERS))
                : new ItemStack(state.getBlock()));
        pickaxe.mineBlock(level, state, pos, player);
        player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
    }
}
