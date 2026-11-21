package net.sorukoma.quasium.golem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.sorukoma.quasium.Config;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class GolemStaffItem extends Item {
    public GolemStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof WorkerGolem golem)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (golem.isOwnedBy(player)) {
            player.getItemInHand(hand).set(MarkBlocks.STAFF_GOLEM.get(), golem.getUUID());
            tell(player, Component.translatable("quasium.staff.picked_golem", golem.getDisplayName()));
        } else {
            tell(player, Component.translatable("quasium.golem.not_owner"));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof SmokeMarkBlockEntity mark) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (player.isSecondaryUseActive()) pickMark(player, stack, mark);
            else bind(player, stack.get(MarkBlocks.STAFF_GOLEM.get()), mark);
            return InteractionResult.CONSUME;
        }
        BlockPos picked = stack.get(MarkBlocks.STAFF_MARK.get());
        if (picked == null) return InteractionResult.PASS;
        if (level.getBlockEntity(picked) instanceof SmokeMarkBlockEntity mark && mark.type() == MarkType.ZONE) {
            if (!level.isClientSide) stretch(player, mark, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        link(player, stack, picked, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            stack.remove(MarkBlocks.STAFF_GOLEM.get());
            stack.remove(MarkBlocks.STAFF_MARK.get());
            tell(player, Component.translatable("quasium.staff.cleared"));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(MarkBlocks.STAFF_GOLEM.get()) || stack.has(MarkBlocks.STAFF_MARK.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.quasium.golem_staff.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.quasium.golem_staff.tooltip.storage").withStyle(ChatFormatting.GRAY));
        if (stack.has(MarkBlocks.STAFF_GOLEM.get())) {
            tooltip.add(Component.translatable("item.quasium.golem_staff.golem").withStyle(ChatFormatting.AQUA));
        }
        BlockPos mark = stack.get(MarkBlocks.STAFF_MARK.get());
        if (mark != null) {
            tooltip.add(Component.translatable("item.quasium.golem_staff.mark", mark.getX(), mark.getY(), mark.getZ())
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    static void bindOnPlacement(Player player, SmokeMarkBlockEntity mark) {
        for (InteractionHand hand : InteractionHand.values()) {
            UUID golem = player.getItemInHand(hand).get(MarkBlocks.STAFF_GOLEM.get());
            if (golem != null) {
                bind(player, golem, mark);
                return;
            }
        }
    }

    private static void bind(Player player, @Nullable UUID golemId, SmokeMarkBlockEntity mark) {
        if (golemId == null) {
            tell(player, Component.translatable("quasium.staff.no_golem"));
            return;
        }
        WorkerGolem golem = ((ServerLevel) player.level()).getEntity(golemId) instanceof WorkerGolem found ? found : null;
        if (GolemBindings.refuse(player, golem, mark)) return;
        boolean bound = GolemBindings.toggle(player, golem, mark);
        int radius = GolemBindings.reach(mark.type());
        boolean far = mark.getBlockPos().distSqr(golem.blockPosition()) > (double) radius * radius;
        String key = !bound ? "quasium.staff.unbound" : far ? "quasium.staff.bound_far" : "quasium.staff.bound";
        tell(player, Component.translatable(key, golem.getDisplayName(), radius));
    }

    private static void pickMark(Player player, ItemStack staff, SmokeMarkBlockEntity mark) {
        if (mark.type() != MarkType.FILL && mark.type() != MarkType.ZONE) {
            tell(player, Component.translatable("quasium.staff.not_storage"));
        } else if (!mark.mayEdit(player)) {
            tell(player, Component.translatable("quasium.staff.not_your_mark"));
        } else {
            staff.set(MarkBlocks.STAFF_MARK.get(), mark.getBlockPos());
            tell(player, Component.translatable(mark.type() == MarkType.ZONE ? "quasium.staff.picked_zone" : "quasium.staff.picked_mark"));
        }
    }

    private static void stretch(Player player, SmokeMarkBlockEntity mark, BlockPos corner) {
        if (!mark.mayEdit(player)) {
            tell(player, Component.translatable("quasium.staff.not_your_mark"));
        } else if (!ZoneBox.fits(mark.getBlockPos(), corner)) {
            tell(player, Component.translatable("quasium.staff.zone_too_big", ZoneBox.MAX_SPAN, ZoneBox.MAX_HEIGHT));
        } else {
            mark.setCorner(player, corner);
            var box = ZoneBox.of(player.level(), mark.getBlockPos());
            tell(player, Component.translatable("quasium.staff.zone_set", box.getXSpan(), box.getYSpan(), box.getZSpan()));
        }
    }

    private static void link(Player player, ItemStack staff, BlockPos fill, BlockPos store) {
        int radius = Config.MARK_LINK_RADIUS.getAsInt();
        if (store.distSqr(fill) > (double) radius * radius) {
            tell(player, Component.translatable("quasium.staff.link_far", radius));
            return;
        }
        if (!(player.level().getBlockEntity(fill) instanceof SmokeMarkBlockEntity mark) || mark.type() != MarkType.FILL) {
            staff.remove(MarkBlocks.STAFF_MARK.get());
            tell(player, Component.translatable("quasium.staff.mark_gone"));
            return;
        }
        if (!mark.mayEdit(player)) {
            tell(player, Component.translatable("quasium.staff.not_your_mark"));
            return;
        }
        mark.pruneLinks();
        int max = Config.MARK_MAX_LINKS.getAsInt();
        if (!mark.links().contains(store) && mark.links().size() >= max) {
            tell(player, Component.translatable("quasium.staff.links_full", max));
            return;
        }
        boolean linked = mark.toggleLink(player, store);
        tell(player, linked ? Component.translatable("quasium.staff.linked", mark.links().size(), max)
                : Component.translatable("quasium.staff.unlinked"));
    }

    private static void tell(Player player, Component message) {
        player.displayClientMessage(message, true);
    }
}
