package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

final class ProgramHands {
    private static final TagKey<Block> CANNOT_MINE = BlockTags.create(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "golem_cannot_mine"));

    enum Result { DONE, SKIP, MISSED, FAIL, STOP }

    private ProgramHands() {
    }

    static Result use(ClayGolemEntity golem, ProgramTask task, boolean simulate) {
        return switch (task.type()) {
            case BREAK -> breakAt(golem, task, simulate);
            case PUT -> put(golem, task, simulate);
            case TAKE -> take(golem, task, simulate);
            case PRESS -> press(golem, task, simulate);
            default -> Result.DONE;
        };
    }

    static Result breakAt(ClayGolemEntity golem, ProgramTask task, boolean simulate) {
        ServerLevel level = (ServerLevel) golem.level();
        BlockPos pos = task.place();
        boolean inArea = task.inArea();
        BlockState state = level.getBlockState(pos);
        String block = state.getBlock().getDescriptionId();
        ItemStack tool = golem.tool();
        if (state.isAir()) return Result.SKIP;
        if (!breakable(level, pos, state)) return inArea ? Result.SKIP : logged(golem, Result.FAIL, "program_protected", block, pos);
        if (state.is(CANNOT_MINE)) return logged(golem, inArea ? Result.SKIP : Result.FAIL, "program_player_only", block);
        if (!EventHooks.canEntityGrief(level, golem)) return logged(golem, Result.STOP, "program_no_griefing");
        if (tool.isEmpty() || tool.is(ModItems.HOOKAH_PIPE.get())) return logged(golem, Result.STOP, "program_no_tool", block);
        if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
            return inArea ? Result.MISSED : logged(golem, Result.FAIL, "program_no_tool", block);
        }
        if (golem.savesTool()) return logged(golem, Result.STOP, "program_tool_worn", tool.getDescriptionId());
        if (!golem.hasRoom()) return logged(golem, Result.STOP, "program_load_full");
        if (simulate) return Result.DONE;
        if (!ProgramClaims.mayBreak(golem, pos, state)) return logged(golem, inArea ? Result.SKIP : Result.FAIL, "program_claimed", block);
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), golem, tool);
        level.destroyBlock(pos, false, golem);
        golem.takeIn(drops, pos);
        GolemTools.use(golem);
        return Result.DONE;
    }

    private static boolean breakable(Level level, BlockPos pos, BlockState state) {
        return state.getDestroySpeed(level, pos) >= 0 && level.getBlockEntity(pos) == null
                && !(level.getBlockState(pos.above()).getBlock() instanceof SmokeMarkBlock)
                && !(state.getBlock() instanceof LiquidBlock) && level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null;
    }

    static Result put(ClayGolemEntity golem, ProgramTask task, boolean simulate) {
        IItemHandler container = container(golem.level(), task.place(), task.face());
        if (container == null) return task.inArea() ? Result.SKIP : logged(golem, Result.FAIL, "program_no_container", task.place());
        if (!ProgramClaims.mayReach(golem, task.place())) return claimed(golem, task);
        Predicate<ItemStack> wanted = wanted(task.block().filter());
        IItemHandler cargo = golem.cargo();
        int limit = limit(task);
        int moved = 0;
        ItemStack refused = null;
        boolean welcome = false;
        for (int slot = 0; slot < cargo.getSlots() && moved < limit; slot++) {
            ItemStack stack = cargo.getStackInSlot(slot);
            if (stack.isEmpty() || !wanted.test(stack)) continue;
            ItemStack offer = stack.copyWithCount(Math.min(limit - moved, stack.getCount()));
            if (accepts(container, offer)) welcome = true;
            else if (refused == null) refused = offer;
            int fits = offer.getCount() - ItemHandlerHelper.insertItemStacked(container, offer, simulate).getCount();
            if (!simulate) cargo.extractItem(slot, fits, false);
            moved += fits;
        }
        if (!welcome && refused == null) return Result.SKIP;
        if (moved > 0) return Result.DONE;
        if (task.inArea()) return Result.SKIP;
        return welcome ? logged(golem, Result.FAIL, "program_container_full", task.place())
                : logged(golem, Result.FAIL, "program_wrong_item", task.place(), refused.getDescriptionId());
    }

    static Result take(ClayGolemEntity golem, ProgramTask task, boolean simulate) {
        IItemHandler container = container(golem.level(), task.place(), Direction.DOWN);
        if (container == null) return task.inArea() ? Result.SKIP : logged(golem, Result.FAIL, "program_no_container", task.place());
        if (!ProgramClaims.mayReach(golem, task.place())) return claimed(golem, task);
        Predicate<ItemStack> wanted = wanted(task.block().filter());
        int limit = limit(task);
        int moved = 0;
        boolean found = false;
        for (int slot = 0; slot < container.getSlots() && moved < limit; slot++) {
            ItemStack offered = container.extractItem(slot, limit - moved, true);
            if (offered.isEmpty() || !wanted.test(offered)) continue;
            found = true;
            int fits = offered.getCount() - golem.stow(offered, true).getCount();
            if (fits <= 0) continue;
            if (!simulate) golem.stow(container.extractItem(slot, fits, false), false);
            moved += fits;
        }
        if (!found) return task.inArea() ? Result.SKIP : logged(golem, Result.FAIL, "program_nothing_to_take", task.place());
        if (moved == 0) return logged(golem, Result.STOP, "program_load_full");
        return Result.DONE;
    }

    static Result press(ClayGolemEntity golem, ProgramTask task, boolean simulate) {
        BlockPos pos = task.place();
        BlockState state = golem.level().getBlockState(pos);
        if (!pressable(state)) return refused(golem, task, state);
        if (!ProgramClaims.mayReach(golem, pos)) return claimed(golem, task);
        if (simulate || pushed(golem, pos, state)) return Result.DONE;
        return refused(golem, task, state);
    }

    private static boolean pressable(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof DoorBlock door) return door.type().canOpenByHand();
        return block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock;
    }

    private static boolean pushed(ClayGolemEntity golem, BlockPos pos, BlockState state) {
        Level level = golem.level();
        switch (state.getBlock()) {
            case ButtonBlock button -> {
                if (!state.getValue(ButtonBlock.POWERED)) button.press(state, level, pos, null);
                return true;
            }
            case LeverBlock lever -> {
                lever.pull(state, level, pos, null);
                return true;
            }
            case DoorBlock door -> {
                door.setOpen(golem, level, state, pos, !door.isOpen(state));
                return true;
            }
            default -> {
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                return state.useWithoutItem(level, ProgramClaims.hand(golem, pos), hit).consumesAction();
            }
        }
    }

    private static Result refused(ClayGolemEntity golem, ProgramTask task, BlockState state) {
        return task.inArea() ? Result.SKIP : logged(golem, Result.FAIL, "program_cannot_press", state.getBlock().getDescriptionId(), task.place());
    }

    private static Result claimed(ClayGolemEntity golem, ProgramTask task) {
        String block = golem.level().getBlockState(task.place()).getBlock().getDescriptionId();
        return logged(golem, task.inArea() ? Result.SKIP : Result.FAIL, "program_claimed", block);
    }

    private static boolean accepts(IItemHandler container, ItemStack stack) {
        for (int slot = 0; slot < container.getSlots(); slot++) {
            if (container.isItemValid(slot, stack)) return true;
        }
        return false;
    }

    private static int limit(ProgramTask task) {
        return task.block().number() == 0 ? Integer.MAX_VALUE : task.block().number();
    }

    @Nullable
    static IItemHandler container(Level level, BlockPos pos, Direction side) {
        return level.isLoaded(pos) ? level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) : null;
    }

    static Predicate<ItemStack> wanted(ItemStack filter) {
        if (filter.isEmpty()) return stack -> true;
        if (filter.isDamageableItem()) return stack -> ItemStack.isSameItem(stack, filter);
        return stack -> ItemStack.isSameItemSameComponents(stack, filter);
    }

    private static Result logged(ClayGolemEntity golem, Result result, String event, Object... args) {
        golem.note(event, args);
        return result;
    }
}
