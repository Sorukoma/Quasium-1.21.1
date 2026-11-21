package net.sorukoma.quasium.ritual.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.ritual.RitualConfig;

public class SignalPipeItem extends ArtifactItem {
    public SignalPipeItem(Properties properties) {
        super(properties, () -> new Object[]{ConfigArgs.synced(RitualConfig.SIGNAL_PIPE_HEIGHT),
                ConfigArgs.synced(RitualConfig.SIGNAL_PIPE_MAX_CHARS), ConfigArgs.synced(RitualConfig.SIGNAL_PIPE_COOLDOWN_SEC)});
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer signaller)) {
            return InteractionResultHolder.success(stack);
        }
        BlockPos pos = player.blockPosition();
        Component text = message(stack);
        server.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("quasium.signal.message",
                player.getDisplayName(), pos.getX(), pos.getY(), pos.getZ(), text).withStyle(ChatFormatting.YELLOW), false);
        SignalColumns.raise(server, pos);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 0.6f);
        player.getCooldowns().addCooldown(this, RitualConfig.SIGNAL_PIPE_COOLDOWN_SEC.getAsInt() * 20);
        SmokecraftTrigger.fire(signaller, "signal_pipe");
        return InteractionResultHolder.consume(stack);
    }

    public static Component message(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) return Component.translatable("quasium.signal.default");
        String text = name.getString().strip();
        int limit = RitualConfig.SIGNAL_PIPE_MAX_CHARS.getAsInt();
        return Component.literal(text.length() > limit ? text.substring(0, limit) : text);
    }
}
