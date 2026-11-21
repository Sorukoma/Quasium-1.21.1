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
import net.sorukoma.quasium.sound.ModSoundEvents;

import java.util.List;

public class SmokeWardenItem extends Item {
    public SmokeWardenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        SmokeWardenEntity warden = WardenGolems.SMOKE_WARDEN.get().create(level, EntityType.createDefaultStackConfig(level, stack, player),
                pos, MobSpawnType.MOB_SUMMONED, true, context.getClickedFace() == Direction.UP);
        if (warden == null || !level.noCollision(warden)) return InteractionResult.FAIL;
        warden.claim(player);
        level.addFreshEntity(warden);
        level.playSound(null, warden.getX(), warden.getY(), warden.getZ(), ModSoundEvents.GOLEM_READY.get(), SoundSource.NEUTRAL, 1f, 1f);
        stack.consume(1, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("quasium.tooltip.smoke_warden").withStyle(ChatFormatting.GRAY));
    }
}
