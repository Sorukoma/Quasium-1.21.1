package net.sorukoma.quasium.ritual.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.cart.HookahCarts;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.component.ModDataComponents.CalloudData;
import net.sorukoma.quasium.component.ModDataComponents.CharcoalData;
import net.sorukoma.quasium.component.ModDataComponents.CharcoalSize;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.item.CalloudItem;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualRegistry;

import java.util.List;
import java.util.Locale;

public class EternalCoalItem extends CalloudItem {
    private static final int HISS_EVERY_TICKS = 80;
    private static final double PIPE_REACH = 3.0;

    public EternalCoalItem(Properties properties) {
        super(properties.component(ModDataComponents.CALLOUD_CONTENTS, embers()));
    }

    public static CalloudData embers() {
        return new CalloudData(List.of(new CharcoalData(CharcoalSize.MM_25, CharcoalSize.MM_25.getMaxTicks(), 100f)),
                List.of(new CharcoalData(CharcoalSize.MM_22, CharcoalSize.MM_22.getMaxTicks(), 50f)));
    }

    @Override
    public boolean burnsForever() {
        return true;
    }

    public static boolean feeds(ServerPlayer player) {
        BackpackHookahState backpack = BackpackHookahState.getState(player.getUUID());
        if (backpack != null && backpack.isPuffing()) return isEternal(backpack.getItemHandler().getStackInSlot(0));
        for (HookahConnectionEntity connection : player.level().getEntitiesOfClass(HookahConnectionEntity.class,
                player.getBoundingBox().inflate(PIPE_REACH), connection -> player.getUUID().equals(connection.getAttachedPlayerUUID())
                        && connection.isPuffing())) {
            if (player.level().getBlockEntity(connection.getHookahBlockPos()) instanceof HookahBlockEntity hookah) {
                return isEternal(hookah.getItemHandler().getStackInSlot(0));
            }
        }
        return player.serverLevel().getEntities(HookahCarts.HOOKAH_MINECART.get(), cart -> cart.isHeldBy(player) && cart.hookah().isPuffing())
                .stream().anyMatch(cart -> isEternal(cart.hookah().getItemHandler().getStackInSlot(0)));
    }

    private static boolean isEternal(ItemStack calloud) {
        return calloud.getItem() instanceof CalloudItem item && item.burnsForever();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide && selected && entity.tickCount % HISS_EVERY_TICKS == 0) {
            level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), RitualRegistry.ETERNAL_COAL_IDLE.get(),
                    SoundSource.PLAYERS, 0.25f, 0.9f + level.random.nextFloat() * 0.2f, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        float heat = stack.getOrDefault(ModDataComponents.CALLOUD_CONTENTS, embers()).totalHeat();
        tooltip.add(Component.translatable("item.quasium.eternal_coal.tooltip", String.format(Locale.ROOT, "%.2f", heat))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.quasium.eternal_coal.price", ConfigArgs.synced(RitualConfig.ETERNAL_COAL_GRAMS_PCT) - 100)
                .withStyle(ChatFormatting.DARK_RED));
    }
}
