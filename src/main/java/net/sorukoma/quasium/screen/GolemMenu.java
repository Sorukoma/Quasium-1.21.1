package net.sorukoma.quasium.screen;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.golem.CalloudGolemEntity;
import net.sorukoma.quasium.golem.ClayGolemEntity;
import net.sorukoma.quasium.golem.GolemJob;
import net.sorukoma.quasium.golem.GolemMood;
import net.sorukoma.quasium.golem.GolemProfile;
import net.sorukoma.quasium.golem.WorkerGolem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GolemMenu extends AbstractContainerMenu {
    public static final int PET_BUTTON = 0;
    public static final int HERD_LESS = 1;
    public static final int HERD_MORE = 2;
    public static final int REFILL_NOW = 3;
    public static final int REFILL_TOGGLE = 4;
    public static final int FEEDER_X = 64;
    public static final int FEEDER_Y = 72;
    public static final int INVENTORY_Y = 104;
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> STATUS_CODEC =
            ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final int INVENTORY_START = 2;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int METER_SCALE = 10;
    private static final ResourceLocation EMPTY_TOOL = ResourceLocation.withDefaultNamespace("item/empty_slot_axe");
    private static final int PROFILE_TICKS = 20;

    @Nullable
    private final WorkerGolem golem;
    private final Player viewer;
    private final ContainerData data = new SimpleContainerData(8);
    private final List<Component> status;
    private GolemProfile profile = GolemProfile.EMPTY;
    private int profileWait;
    private boolean careShown = true;
    private boolean workShown;

    public GolemMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player.level().getEntity(buf.readVarInt()) instanceof WorkerGolem golem ? golem : null,
                WorkerGolem.newFeeder(), new ItemStackHandler(1), STATUS_CODEC.decode(buf));
    }

    public GolemMenu(int containerId, Inventory inventory, WorkerGolem golem) {
        this(containerId, inventory, golem, golem.feeder(), golem.hand(), List.of());
    }

    private GolemMenu(int containerId, Inventory inventory, @Nullable WorkerGolem golem, IItemHandler feeder, IItemHandler hand,
                      List<Component> status) {
        super(ModMenuTypes.GOLEM_MENU.get(), containerId);
        this.golem = golem;
        this.viewer = inventory.player;
        this.status = status;
        addSlot(new SlotItemHandler(feeder, 0, FEEDER_X, FEEDER_Y) {
            @Override
            public boolean isActive() {
                return careShown;
            }
        });
        addSlot(new SlotItemHandler(hand, 0, FEEDER_X, FEEDER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GolemJob.isTool(stack);
            }

            @Override
            public boolean isActive() {
                return workShown;
            }
        }.setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_TOOL));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(playerSlot(inventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(playerSlot(inventory, column, 8 + column * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
        readGolem();
    }

    private Slot playerSlot(Container inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean isActive() {
                return careShown || workShown;
            }
        };
    }

    public static void writeOpening(RegistryFriendlyByteBuf buf, WorkerGolem golem, List<Component> status) {
        buf.writeVarInt(golem.getId());
        STATUS_CODEC.encode(buf, status);
    }

    @Nullable
    public WorkerGolem golem() {
        return golem;
    }

    public List<Component> status() {
        return status;
    }

    public GolemProfile profile() {
        return profile;
    }

    public void receive(GolemProfile received) {
        profile = received;
    }

    public void resendProfile() {
        profileWait = 0;
    }

    public void showTab(boolean care, boolean work) {
        careShown = care;
        workShown = work;
    }

    public float meter() {
        return data.get(0) / (float) METER_SCALE;
    }

    public int productivity() {
        return data.get(1);
    }

    public int misses() {
        return data.get(2);
    }

    public int streak() {
        return data.get(3);
    }

    public int petCooldown() {
        return data.get(4);
    }

    public int state() {
        return Math.max(GolemMood.BEST, data.get(5));
    }

    public int herdLimit() {
        return data.get(6);
    }

    public boolean refillPaused() {
        return data.get(7) != 0;
    }

    private void readGolem() {
        if (golem == null || !(golem.level() instanceof ServerLevel)) return;
        GolemMood mood = golem.mood();
        data.set(0, Math.round(mood.meter() * METER_SCALE));
        data.set(1, mood.productivity());
        data.set(2, mood.misses());
        data.set(3, mood.streak());
        data.set(4, golem.petCooldown());
        data.set(5, mood.state());
        if (golem instanceof ClayGolemEntity clay) data.set(6, clay.herdLimit());
        if (golem instanceof CalloudGolemEntity calloud) data.set(7, calloud.refillPaused() ? 1 : 0);
    }

    private void sendProfile() {
        if (golem == null || !(viewer instanceof ServerPlayer player) || --profileWait > 0) return;
        profileWait = PROFILE_TICKS;
        GolemProfile fresh = GolemProfile.of(containerId, golem, player);
        if (fresh.equals(profile)) return;
        profile = fresh;
        PacketDistributor.sendToPlayer(player, fresh);
    }

    @Override
    public void broadcastChanges() {
        readGolem();
        sendProfile();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (golem == null || !golem.isOwnedBy(player)) return false;
        return switch (id) {
            case PET_BUTTON -> golem.pet(player);
            case HERD_LESS, HERD_MORE -> golem instanceof ClayGolemEntity clay && clay.adjustHerd(id == HERD_MORE);
            case REFILL_NOW -> golem instanceof CalloudGolemEntity calloud && calloud.requestRefill();
            case REFILL_TOGGLE -> golem instanceof CalloudGolemEntity calloud && calloud.toggleRefill();
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean fits;
        if (index < INVENTORY_START) {
            fits = moveItemStackTo(stack, INVENTORY_START, slots.size(), true);
        } else {
            fits = moveItemStackTo(stack, 0, INVENTORY_START, false)
                    || (index < HOTBAR_START ? moveItemStackTo(stack, HOTBAR_START, slots.size(), false)
                    : moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false));
        }
        if (!fits) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return golem != null && golem.isAlive() && player.canInteractWithEntity(golem, 4.0);
    }
}
