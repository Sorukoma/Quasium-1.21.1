package net.sorukoma.quasium.golem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.sorukoma.quasium.sound.ModSoundEvents;

import java.util.List;

public class ClayGolemItem extends Item {
    public ClayGolemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        BlockPos spot = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(context.getClickedFace());
        boolean onTop = !spot.equals(clicked) && context.getClickedFace() == Direction.UP;
        ClayGolemEntity golem = ClayGolems.CLAY_GOLEM.get().spawn(level, context.getItemInHand(), context.getPlayer(), spot,
                MobSpawnType.MOB_SUMMONED, true, onTop);
        if (golem == null) return InteractionResult.FAIL;
        golem.claim(context.getPlayer());
        level.playSound(null, spot, ModSoundEvents.GOLEM_READY.get(), SoundSource.NEUTRAL, 1f, 1f);
        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spot);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.quasium.clay_golem.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
