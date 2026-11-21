package net.sorukoma.quasium.golem.program;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class PlaceMarkerItem extends Item {
    public PlaceMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).getBlock() instanceof ProgramTableBlock) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Direction face = context.getClickedFace();
        stack.set(ProgramBlocks.MARKED_PLACE.get(), new MarkedPlace(GlobalPos.of(level.dimension(), pos.immutable()), face));
        Player player = context.getPlayer();
        if (player != null) {
            player.displayClientMessage(Component.translatable("quasium.place_marker.stored", pos.getX(), pos.getY(), pos.getZ(), face(face)), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || !stack.has(ProgramBlocks.MARKED_PLACE.get())
                || getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            stack.remove(ProgramBlocks.MARKED_PLACE.get());
            player.displayClientMessage(Component.translatable("quasium.place_marker.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ProgramBlocks.MARKED_PLACE.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.quasium.place_marker.tooltip").withStyle(ChatFormatting.GRAY));
        MarkedPlace place = stack.get(ProgramBlocks.MARKED_PLACE.get());
        if (place == null) return;
        BlockPos pos = place.where().pos();
        tooltip.add(Component.translatable("item.quasium.place_marker.place", pos.getX(), pos.getY(), pos.getZ(), face(place.face()))
                .withStyle(ChatFormatting.AQUA));
        if (place.where().dimension() == Level.OVERWORLD) return;
        ResourceLocation dimension = place.where().dimension().location();
        tooltip.add(Component.translatable("item.quasium.place_marker.dimension",
                Component.translatableWithFallback("quasium.dimension." + dimension.toLanguageKey(), dimension.toString()))
                .withStyle(ChatFormatting.AQUA));
    }

    private static Component face(Direction face) {
        return Component.translatable("quasium.place_marker.face." + face.getSerializedName());
    }
}
