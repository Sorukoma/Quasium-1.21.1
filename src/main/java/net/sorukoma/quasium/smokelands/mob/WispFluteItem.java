package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class WispFluteItem extends Item {
    private static final int COOLDOWN = 20;

    public WispFluteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack flute = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) play(server, player, flute);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(flute, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.quasium.wisp_flute.tooltip").withStyle(ChatFormatting.GRAY));
    }

    public static void play(ServerLevel level, Player player, ItemStack flute) {
        UUID out = flute.get(SmokelandsEntities.FLUTE_WISP.get());
        Entity summoned = out == null ? null : level.getEntity(out);
        if (summoned instanceof DraftWisp wisp && wisp.isAlive()) {
            wisp.discard();
            flute.remove(SmokelandsEntities.FLUTE_WISP.get());
            player.displayClientMessage(Component.translatable("item.quasium.wisp_flute.recalled"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1f, 0.7f);
            return;
        }
        DraftWisp wisp = SmokelandsEntities.DRAFT_WISP.get().create(level);
        if (wisp == null) return;
        wisp.serve(player);
        wisp.moveTo(player.getX(), player.getEyeY() + 0.5, player.getZ(), player.getYRot(), 0f);
        level.addFreshEntity(wisp);
        flute.set(SmokelandsEntities.FLUTE_WISP.get(), wisp.getUUID());
        player.displayClientMessage(Component.translatable("item.quasium.wisp_flute.released"), true);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1f, 1.2f);
    }
}
