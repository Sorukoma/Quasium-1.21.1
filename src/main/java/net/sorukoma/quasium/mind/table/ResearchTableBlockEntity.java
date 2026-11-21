package net.sorukoma.quasium.mind.table;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.progression.ConfigArgs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int PAPER = 0;
    public static final int INK = 1;
    public static final int OUTPUT = 2;

    private int progress;
    private int queued;
    @Nullable
    private UUID writer;

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case PAPER -> stack.is(Items.PAPER);
                case INK -> stack.is(Items.INK_SAC);
                default -> stack.is(MindRegistry.TREATISE.get());
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : queued;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(MindRegistry.RESEARCH_TABLE_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public static int writeTicks() {
        return ConfigArgs.synced(MindConfig.TREATISE_SECONDS) * 20;
    }

    private boolean stocked() {
        ItemStack output = items.getStackInSlot(OUTPUT);
        return !items.getStackInSlot(PAPER).isEmpty() && !items.getStackInSlot(INK).isEmpty()
                && output.getCount() < output.getMaxStackSize();
    }

    public boolean order(ServerPlayer player) {
        int price = MindConfig.TREATISE_INSIGHT.getAsInt();
        int pairs = Math.min(items.getStackInSlot(PAPER).getCount(), items.getStackInSlot(INK).getCount());
        if (queued >= pairs || !stocked()) {
            player.displayClientMessage(Component.translatable("quasium.research_table.no_paper"), true);
            return false;
        }
        if (PlayerMagicData.getInsight(player) < price) {
            player.displayClientMessage(Component.translatable("quasium.research_table.no_insight", price), true);
            return false;
        }
        PlayerMagicData.addInsight(player, -price);
        queued++;
        writer = player.getUUID();
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        if (table.queued <= 0 || !table.stocked()) return;
        if (table.progress % 20 == 0) {
            level.playSound(null, pos, MindRegistry.TREATISE_WRITE.get(), SoundSource.BLOCKS, 0.7f, 0.9f + level.random.nextFloat() * 0.2f);
        }
        if (++table.progress >= writeTicks()) table.finish(level);
        table.setChanged();
    }

    private void finish(Level level) {
        progress = 0;
        queued--;
        items.extractItem(PAPER, 1, false);
        items.extractItem(INK, 1, false);
        items.insertItem(OUTPUT, new ItemStack(MindRegistry.TREATISE.get()), false);
        if (writer != null && level.getPlayerByUUID(writer) instanceof ServerPlayer player) SmokecraftTrigger.fire(player, "treatise");
    }

    public void dropContents() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Queued", queued);
        if (writer != null) tag.putUUID("Writer", writer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("Progress");
        queued = tag.getInt("Queued");
        writer = tag.hasUUID("Writer") ? tag.getUUID("Writer") : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.research_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ResearchTableMenu(containerId, inventory, this, data);
    }
}
