package net.sorukoma.quasium.golem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.sorukoma.quasium.sound.ModSoundEvents;

import java.util.List;
import java.util.function.Consumer;

public class CalloudGolemItem extends Item {
    public CalloudGolemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Consumer<CalloudGolemEntity> setup = EntityType.appendDefaultStackConfig(golem -> golem.claim(player), level, stack, player);
        boolean raised = !pos.equals(clicked) && face == Direction.UP;
        if (CalloudGolems.CALLOUD_GOLEM.get().spawn(level, setup, pos, MobSpawnType.MOB_SUMMONED, true, raised) == null) {
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        level.playSound(null, pos, ModSoundEvents.GOLEM_READY.get(), SoundSource.NEUTRAL, 1f, 1f);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("quasium.tooltip.calloud_golem").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("quasium.tooltip.calloud_golem.marks").withStyle(ChatFormatting.GRAY));
    }
}
