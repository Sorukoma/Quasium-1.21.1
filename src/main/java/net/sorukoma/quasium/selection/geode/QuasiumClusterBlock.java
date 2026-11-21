package net.sorukoma.quasium.selection.geode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.QuasiumOreBlock;
import net.sorukoma.quasium.client.geode.GeodeAmbience;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.selection.SelectionConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class QuasiumClusterBlock extends AmethystClusterBlock {
    public QuasiumClusterBlock(float height, float aabbOffset, Properties properties) {
        super(height, aabbOffset, properties);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!(player instanceof ServerPlayer serverPlayer) || QuasiumOreBlock.hasSilkTouch(player, tool)) return;
        if (PlayerKaifData.getKaifValue(player) < SelectionConfig.CLUSTER_DROP_MIN_KAIF.get()) {
            player.displayClientMessage(Component.translatable("quasium.message.quasium_ore.low_kaif"), true);
        } else {
            SmokecraftTrigger.fire(serverPlayer, "geode");
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player player)) return List.of();
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && QuasiumOreBlock.hasSilkTouch(player, tool)) return List.of(new ItemStack(this));
        int count = GeodeDrops.clusterCount(PlayerKaifData.getKaifValue(player), player.getRandom());
        return count > 0 ? List.of(new ItemStack(ModItems.RAWQUASIUM.get(), count)) : List.of();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        GeodeAmbience.animate(level, pos, random);
    }
}
