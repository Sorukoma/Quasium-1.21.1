package net.sorukoma.quasium.block;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerOreLuckData;
import net.sorukoma.quasium.item.ModItems;

import java.util.Collections;
import java.util.List;

public class QuasiumOreBlock extends Block {
    private static final int MIN_KAIF_FOR_DROP = 10;

    public QuasiumOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void playerDestroy(net.minecraft.world.level.Level level, Player player, net.minecraft.core.BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) return;

        if (hasSilkTouch(serverPlayer, tool)) return;

        int kaif = PlayerKaifData.getKaifValue(player);
        if (kaif < MIN_KAIF_FOR_DROP) {
            player.displayClientMessage(Component.translatable("quasium.message.quasium_ore.low_kaif"), true);
        } else {
            int fortuneLevel = Math.min(3, (kaif - MIN_KAIF_FOR_DROP) / 30);
            PlayerOreLuckData.addAchievedFortuneLevel(serverPlayer, fortuneLevel);
            QuasiumModAdvancements.QUASIUM_ORE_MINED.get().trigger(serverPlayer);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder paramsBuilder) {
        LootParams params = paramsBuilder.withParameter(LootContextParams.BLOCK_STATE, state)
                .create(LootContextParamSets.BLOCK);

        Entity entity = params.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof Player player)) {
            return Collections.emptyList();
        }

        ItemStack tool = params.getParamOrNull(LootContextParams.TOOL);
        if (tool != null && hasSilkTouch(player, tool)) {
            return List.of(new ItemStack(this));
        }

        int kaif = PlayerKaifData.getKaifValue(player);
        if (kaif < MIN_KAIF_FOR_DROP) {
            return Collections.emptyList();
        }

        int fortuneLevel = Math.min(3, (kaif - MIN_KAIF_FOR_DROP) / 30);
        int count = 1;
        if (fortuneLevel > 0) {
            count = 1 + player.level().random.nextInt(fortuneLevel + 1);
        }

        return List.of(new ItemStack(ModItems.RAWQUASIUM.get(), count));
    }

    private static boolean hasSilkTouch(Player player, ItemStack tool) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        HolderLookup.RegistryLookup<Enchantment> registryLookup =
                serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> silkTouchHolder = registryLookup.getOrThrow(Enchantments.SILK_TOUCH);
        return tool.getEnchantmentLevel(silkTouchHolder) > 0;
    }
}