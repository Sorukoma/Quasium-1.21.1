package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

final class ToolkeeperGoal extends Goal {
    private static final double REACH = 2.8;
    private static final int RESCAN_TICKS = 40;

    private final ClayGolemEntity golem;
    private final ClayTrek trek;
    @Nullable
    private BlockPos store;
    private int nextScan;
    private boolean warned;

    ToolkeeperGoal(ClayGolemEntity golem) {
        this.golem = golem;
        this.trek = new ClayTrek(golem);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (GolemPrograms.running(golem) || golem.tickCount < nextScan) return false;
        if (!golem.savesTool()) {
            warned = false;
            return false;
        }
        nextScan = golem.tickCount + golem.paced(RESCAN_TICKS);
        store = pickStore();
        if (store == null) warnNowhere();
        return store != null;
    }

    private void warnNowhere() {
        golem.waitFor("tool_nowhere");
        Player owner = golem.ownerId() == null ? null : golem.level().getPlayerByUUID(golem.ownerId());
        if (warned || owner == null) return;
        warned = true;
        owner.displayClientMessage(Component.translatable("quasium.golem.tool_nowhere", golem.getDisplayName(), golem.tool().getHoverName()), false);
    }

    @Override
    public boolean canContinueToUse() {
        return store != null && golem.savesTool();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        golem.note("tool_trip", golem.tool().getDescriptionId(), store);
        trek.to(Vec3.atCenterOf(store));
    }

    @Override
    public void stop() {
        store = null;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (store == null) return;
        golem.getLookControl().setLookAt(Vec3.atCenterOf(store));
        if (trek.arrived(REACH)) {
            swap(store);
            store = null;
        } else if (trek.stuck()) {
            golem.markUnreachable(store);
            store = null;
        }
    }

    private boolean fresh(ItemStack stack) {
        return stack.isDamageableItem() && GolemJob.isTool(stack) && GolemJob.of(stack) == golem.job() && !GolemTools.worn(stack);
    }

    @Nullable
    private BlockPos pickStore() {
        BlockPos roomy = null;
        for (BlockPos pos : ClayStores.of(golem)) {
            IItemHandler items = ClayStores.handler(golem.level(), pos);
            if (items == null || golem.isUnreachable(pos)) continue;
            if (ClayStores.slotOf(items, this::fresh) >= 0) return pos;
            if (roomy == null && ItemHandlerHelper.insertItemStacked(items, golem.tool(), true).isEmpty()) roomy = pos;
        }
        return roomy;
    }

    private void swap(BlockPos pos) {
        IItemHandler items = ClayStores.handler(golem.level(), pos);
        if (items == null) return;
        ItemStack worn = golem.tool().copy();
        int slot = ClayStores.slotOf(items, this::fresh);
        ItemStack fresh = slot < 0 ? ItemStack.EMPTY : items.extractItem(slot, 1, false);
        if (!ItemHandlerHelper.insertItemStacked(items, worn, false).isEmpty()) {
            ItemHandlerHelper.insertItemStacked(items, fresh, false);
            return;
        }
        golem.setItemSlot(EquipmentSlot.MAINHAND, fresh);
        golem.note("tool_stored", worn.getDescriptionId(), pos);
        if (fresh.isEmpty()) golem.note("no_fresh_tool");
        else golem.note("tool_taken", fresh.getDescriptionId());
    }
}
