package net.sorukoma.quasium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerFlavorPreferenceData;
import net.sorukoma.quasium.data.PlayerTobaccoData;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.sound.ModSoundEvents;
import net.sorukoma.quasium.util.SmokeColorUtil;

import java.util.List;

public class AromaVialItem extends Item {
    public static final int SNIFF_TICKS = 36;

    public AromaVialItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(String flavor) {
        ItemStack vial = new ItemStack(ModItems.AROMA_VIAL.get());
        vial.set(ModDataComponents.ESSENCE_TYPE.get(), flavor);
        return vial;
    }

    public static String flavorOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "");
    }

    @Override
    public Component getName(ItemStack stack) {
        String flavor = flavorOf(stack);
        if (flavor.isEmpty()) return super.getName(stack);
        return Component.translatable("item.quasium.aroma_vial.flavored", Component.translatable("quasium.flavor." + flavor));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("quasium.tooltip.aroma_vial").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return SNIFF_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack vial = player.getItemInHand(hand);
        if (flavorOf(vial).isEmpty()) return InteractionResultHolder.pass(vial);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.5f, 1.6f);
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int used = SNIFF_TICKS - remaining;
        if (used % 12 == 4) {
            entity.playSound(SoundEvents.FOX_SNIFF, 0.8f, 0.9f + entity.getRandom().nextFloat() * 0.3f);
        }
        if (!level.isClientSide() || used % 3 != 0) return;
        Vec3 nose = entity.getEyePosition().add(entity.getLookAngle().scale(0.25)).subtract(0, 0.1, 0);
        Vec3 vial = entity.getEyePosition().add(entity.getLookAngle().scale(0.6)).subtract(0, 0.25, 0);
        Vec3 drift = nose.subtract(vial).scale(0.08);
        level.addParticle(ModParticles.noteSmoke(SmokeColorUtil.colorForFlavor(flavorOf(stack))),
                vial.x, vial.y, vial.z, drift.x, drift.y + 0.01, drift.z);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        String flavor = flavorOf(stack);
        if (entity instanceof ServerPlayer player && !flavor.isEmpty()) react(player, flavor);
        if (!(entity instanceof Player player) || player.hasInfiniteMaterials()) return stack;
        return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE), false);
    }

    private static void react(ServerPlayer player, String flavor) {
        Verdict verdict = Perk.SOMMELIER.ownedBy(player) ? Verdict.of(PlayerFlavorPreferenceData.score(player, flavor)) : Verdict.UNTOLD;
        if (verdict != Verdict.UNTOLD) PlayerTobaccoData.addFirstFlavorResult(player, flavor);
        player.displayClientMessage(Component.translatable("quasium.aroma." + verdict.key,
                Component.translatable("quasium.flavor." + flavor)).withStyle(verdict.color), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), verdict.sound(), SoundSource.PLAYERS, 0.8f, verdict.pitch);
        Vec3 head = player.getEyePosition().add(0, 0.35, 0);
        player.serverLevel().sendParticles(verdict.particle, head.x, head.y, head.z, verdict.particles, 0.3, 0.15, 0.3, 0.02);
    }

    private enum Verdict {
        LOVE("love", ChatFormatting.GREEN, ParticleTypes.HEART, 5, 1.2f),
        LIKE("like", ChatFormatting.DARK_GREEN, ParticleTypes.HAPPY_VILLAGER, 6, 1.0f),
        MEH("meh", ChatFormatting.GRAY, ParticleTypes.SMOKE, 6, 1.0f),
        HATE("hate", ChatFormatting.RED, ParticleTypes.ANGRY_VILLAGER, 3, 1.0f),
        UNTOLD("untold", ChatFormatting.GRAY, ParticleTypes.SMOKE, 4, 0.8f);

        private final String key;
        private final ChatFormatting color;
        private final ParticleOptions particle;
        private final int particles;
        private final float pitch;

        Verdict(String key, ChatFormatting color, ParticleOptions particle, int particles, float pitch) {
            this.key = key;
            this.color = color;
            this.particle = particle;
            this.particles = particles;
            this.pitch = pitch;
        }

        static Verdict of(int score) {
            if (score >= 2) return LOVE;
            if (score == 1) return LIKE;
            return score <= -2 ? HATE : MEH;
        }

        SoundEvent sound() {
            return switch (this) {
                case LOVE -> SoundEvents.AMETHYST_BLOCK_CHIME;
                case LIKE -> SoundEvents.NOTE_BLOCK_BELL.value();
                case MEH, UNTOLD -> SoundEvents.CANDLE_EXTINGUISH;
                case HATE -> ModSoundEvents.COUGH.get();
            };
        }
    }
}
