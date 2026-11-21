package net.sorukoma.quasium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.draft.SmokePipeBlock;
import net.sorukoma.quasium.hookah.HookahReport;
import net.sorukoma.quasium.magic.NoteScanner;

public class KaifometerItem extends Item {

    public KaifometerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Block clicked = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof HookahBlockEntity hookah) {
            if (context.getPlayer() instanceof ServerPlayer player) HookahReport.of(player.serverLevel(), hookah).forEach(player::sendSystemMessage);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (clicked instanceof SmokePipeBlock) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                SmokePipeBlock.report(player.serverLevel(), context.getClickedPos()).forEach(player::sendSystemMessage);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        ItemStack block = new ItemStack(clicked.asItem());
        if (block.isEmpty()) return InteractionResult.PASS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            NoteScanner.scanItem(player, block, NoteScanner.Kind.BLOCK);
            player.sendSystemMessage(net.sorukoma.quasium.ecology.SmokeDensity.describe(context.getLevel(), context.getClickedPos()));
            player.sendSystemMessage(net.sorukoma.quasium.ecology.SmogSpread.describe(player.serverLevel()));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) NoteScanner.scanEntity(serverPlayer, target);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.isEmpty()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player instanceof ServerPlayer serverPlayer) NoteScanner.scanItem(serverPlayer, other, NoteScanner.Kind.ITEM);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack kaifometer, ItemStack carried, Slot slot, ClickAction action,
                                            Player player, SlotAccess carriedAccess) {
        if (action != ClickAction.SECONDARY || carried.isEmpty()) return false;
        if (player instanceof ServerPlayer serverPlayer) NoteScanner.scanItem(serverPlayer, carried, NoteScanner.Kind.ITEM);
        return true;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack kaifometer, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.hasItem()) return false;
        if (player instanceof ServerPlayer serverPlayer) NoteScanner.scanItem(serverPlayer, slot.getItem(), NoteScanner.Kind.ITEM);
        return true;
    }
}
