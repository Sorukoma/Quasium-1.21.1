package net.sorukoma.quasium.cart;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class HookahMinecartItem extends Item {
    private static final double RAIL_HEIGHT = 0.0625;
    private static final double SLOPE_LIFT = 0.5;

    public HookahMinecartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState rail = level.getBlockState(pos);
        if (!rail.is(BlockTags.RAILS)) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        if (level instanceof ServerLevel server) {
            HookahMinecart cart = HookahCarts.HOOKAH_MINECART.get().create(server);
            if (cart == null) return InteractionResult.FAIL;
            boolean slope = rail.getBlock() instanceof BaseRailBlock railBlock && railBlock.getRailDirection(rail, level, pos, null).isAscending();
            cart.moveTo(pos.getX() + 0.5, pos.getY() + RAIL_HEIGHT + (slope ? SLOPE_LIFT : 0), pos.getZ() + 0.5);
            EntityType.<HookahMinecart>createDefaultStackConfig(server, stack, context.getPlayer()).accept(cart);
            server.addFreshEntity(cart);
            server.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), server.getBlockState(pos.below())));
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
