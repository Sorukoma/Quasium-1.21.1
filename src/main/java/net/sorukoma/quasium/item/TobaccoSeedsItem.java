package net.sorukoma.quasium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.block.entity.TobaccoCropBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.selection.genetics.Lineage;
import net.sorukoma.quasium.selection.quality.LeafQuality;

import java.util.List;
import java.util.function.Supplier;

public class TobaccoSeedsItem extends Item {

    private final Supplier<Block> cropBlock;

    public TobaccoSeedsItem(Supplier<Block> cropBlock, Properties properties) {
        super(properties);
        this.cropBlock = cropBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(cropBlock.get() instanceof TobaccoCropBlock crop)) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos soil = context.getClickedPos();
        BlockPos above = soil.above();
        ItemStack held = context.getItemInHand();
        String mutation = held.getOrDefault(ModDataComponents.TOBACCO_MUTATION.get(), "");
        BlockState planted = crop.defaultBlockState().setValue(TobaccoCropBlock.MUTATION, TobaccoCropBlock.mutationCode(mutation));
        if (!level.getBlockState(soil).is(TobaccoVarieties.soil(crop.getVariety()))
                || !level.getBlockState(above).canBeReplaced() || !planted.canSurvive(level, above)) {
            return InteractionResult.PASS;
        }
        level.setBlockAndUpdate(above, planted);
        if (level.getBlockEntity(above) instanceof TobaccoCropBlockEntity be) {
            String variety = crop.getVariety();
            be.setStars(held.getOrDefault(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), TobaccoVarieties.strength(variety)),
                    held.getOrDefault(ModDataComponents.TOBACCO_YIELD_STARS.get(), TobaccoVarieties.yield(variety)),
                    held.getOrDefault(ModDataComponents.TOBACCO_QUALITY_STARS.get(), TobaccoVarieties.quality(variety)));
            be.setLineage(Lineage.of(held));
        }
        level.gameEvent(GameEvent.BLOCK_CHANGE, above, GameEvent.Context.of(context.getPlayer(), planted));
        if (context.getPlayer() != null && !context.getPlayer().isCreative()) held.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String variety = TobaccoVarieties.getVariety(stack);
        int strength = stack.getOrDefault(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), TobaccoVarieties.strength(variety));
        int yield = stack.getOrDefault(ModDataComponents.TOBACCO_YIELD_STARS.get(), TobaccoVarieties.yield(variety));
        int quality = stack.getOrDefault(ModDataComponents.TOBACCO_QUALITY_STARS.get(), TobaccoVarieties.quality(variety));

        tooltip.add(Component.translatable("quasium.tooltip.tobacco_variety",
                Lineage.displayName(Lineage.of(stack))).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("quasium.tooltip.tobacco_stats",
                LeafQuality.stars(strength), LeafQuality.stars(yield), LeafQuality.stars(quality)).withStyle(ChatFormatting.GOLD));

        String mutation = stack.getOrDefault(ModDataComponents.TOBACCO_MUTATION.get(), "");
        if (!mutation.isEmpty()) {
            tooltip.add(Component.translatable("quasium.tooltip.tobacco_mutation",
                    Component.translatable("quasium.mutation." + mutation)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
