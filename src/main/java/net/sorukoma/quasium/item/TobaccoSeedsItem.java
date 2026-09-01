package net.sorukoma.quasium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.component.ModDataComponents;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.function.Supplier;

public class TobaccoSeedsItem extends Item {

    private final Supplier<net.minecraft.world.level.block.Block> cropBlock;

    public TobaccoSeedsItem(Supplier<net.minecraft.world.level.block.Block> cropBlock, Properties properties) {
        super(properties);
        this.cropBlock = cropBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.FARMLAND)) {
            BlockPos abovePos = pos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            if (aboveState.isAir() || aboveState.canBeReplaced()) {
                BlockState planted = cropBlock.get().defaultBlockState();
                String variety = TobaccoVarieties.DIKAR;
                if (planted.getBlock() instanceof TobaccoCropBlock crop) {
                    String mutation = context.getItemInHand().getOrDefault(ModDataComponents.TOBACCO_MUTATION.get(), "");
                    planted = planted.setValue(TobaccoCropBlock.MUTATION, TobaccoCropBlock.mutationCode(mutation));
                    variety = crop.getVariety();
                }
                level.setBlockAndUpdate(abovePos, planted);
                if (level.getBlockEntity(abovePos) instanceof net.sorukoma.quasium.block.entity.TobaccoCropBlockEntity be) {
                    ItemStack held = context.getItemInHand();
                    int strength = held.getOrDefault(ModDataComponents.TOBACCO_STRENGTH_STARS.get(),
                            TobaccoVarieties.strength(variety));
                    int yield = held.getOrDefault(ModDataComponents.TOBACCO_YIELD_STARS.get(),
                            TobaccoVarieties.yield(variety));
                    int quality = held.getOrDefault(ModDataComponents.TOBACCO_QUALITY_STARS.get(),
                            TobaccoVarieties.quality(variety));
                    be.setStars(strength, yield, quality);
                }
                if (level instanceof ServerLevel) {
                    level.gameEvent(GameEvent.BLOCK_CHANGE, abovePos,
                            net.minecraft.world.level.gameevent.GameEvent.Context.of(context.getPlayer(), planted));
                }
                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        String variety = stack.get(ModDataComponents.TOBACCO_VARIETY.get());
        if (variety == null || variety.isEmpty()) {
            variety = TobaccoVarieties.getVariety(stack);
        }

        int strength = stack.getOrDefault(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), TobaccoVarieties.strength(variety));
        int yield = stack.getOrDefault(ModDataComponents.TOBACCO_YIELD_STARS.get(), TobaccoVarieties.yield(variety));
        int quality = stack.getOrDefault(ModDataComponents.TOBACCO_QUALITY_STARS.get(), TobaccoVarieties.quality(variety));

        tooltip.add(Component.translatable("quasium.tooltip.tobacco_variety",
                Component.translatable("quasium.variety." + variety)).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("quasium.tooltip.tobacco_stats",
                stars(strength), stars(yield), stars(quality)).withStyle(ChatFormatting.GOLD));

        String mutation = stack.getOrDefault(ModDataComponents.TOBACCO_MUTATION.get(), "");
        if (!mutation.isEmpty()) {
            tooltip.add(Component.translatable("quasium.tooltip.tobacco_mutation",
                    Component.translatable("quasium.mutation." + mutation)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static String stars(int value) {
        return "★".repeat(Math.max(0, value)) + "☆".repeat(Math.max(0, 5 - value));
    }
}