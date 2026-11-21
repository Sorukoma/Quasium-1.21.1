package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.sorukoma.quasium.block.entity.EssencePressBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.mind.dream.Dreams;
import net.sorukoma.quasium.item.ModItemTags;
import org.jetbrains.annotations.Nullable;

public class EssencePressBlock extends BaseEntityBlock {
    public EssencePressBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EssencePressBlockEntity e)) return ItemInteractionResult.FAIL;

        String ingredientType = getIngredientType(heldItem);
        if (ingredientType != null) {
            int accepted = e.addIngredients(ingredientType, player.isShiftKeyDown() ? heldItem.getCount() : 1);
            if (accepted > 0) {
                if (!player.isCreative()) heldItem.shrink(accepted);
                player.displayClientMessage(Component.translatable("quasium.message.essence_press.added",
                        Component.translatable("item.quasium.essence_bottle." + ingredientType), accepted,
                        e.getInputCount()), true);
            } else {
                player.displayClientMessage(Component.translatable("quasium.message.essence_press.wrong_input"), true);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (heldItem.is(Items.GLASS_BOTTLE)) {
            if (e.getEssenceAmount() > 0) {
                ItemStack out = new ItemStack(ModItems.ESSENCE_BOTTLE.get());
                out.set(ModDataComponents.ESSENCE_TYPE.get(), e.getEssenceType());
                out.set(ModDataComponents.ESSENCE_AMOUNT.get(), Dreams.inspired(player, e.getEssenceAmount()));
                player.getInventory().placeItemBackInInventory(out);
                e.consumeEssence();
                if (!player.isCreative()) heldItem.shrink(1);
                return ItemInteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("quasium.message.essence_press.empty"), true);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EssencePressBlockEntity press)) return InteractionResult.FAIL;

        if (press.getEssenceAmount() > 0) {
            player.displayClientMessage(Component.translatable("quasium.message.essence_press.ready",
                    Component.translatable("item.quasium.essence_bottle." + press.getEssenceType()),
                    press.getEssenceAmount()), true);
        } else if (press.getInputCount() > 0) {
            player.displayClientMessage(Component.translatable("quasium.message.essence_press.progress",
                    Component.translatable("item.quasium.essence_bottle." + press.getInputType()),
                    press.getInputCount()), true);
        } else {
            player.displayClientMessage(Component.translatable("quasium.message.essence_press.status_empty"), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static String getIngredientType(ItemStack stack) {
        if (stack.is(ModItemTags.ESSENCE_MINT)) return "mint";
        if (stack.is(ModItemTags.ESSENCE_BERRY)) return "berry";
        if (stack.is(ModItemTags.ESSENCE_LEMON)) return "lemon";
        if (stack.is(ModItemTags.ESSENCE_LIME)) return "lime";
        if (stack.is(ModItemTags.ESSENCE_ORANGE)) return "orange";
        if (stack.is(ModItemTags.ESSENCE_GRAPEFRUIT)) return "grapefruit";
        if (stack.is(ModItemTags.ESSENCE_GRAPE)) return "grape";
        if (stack.is(ModItemTags.ESSENCE_COCONUT)) return "coconut";
        if (stack.is(ModItemTags.ESSENCE_ANISE)) return "anise";
        if (stack.is(ModItemTags.ESSENCE_ICE)) return "ice";
        if (stack.is(ModItemTags.ESSENCE_HONEY)) return "honey";
        if (stack.is(ModItemTags.ESSENCE_NETHER)) return "nether";
        if (stack.is(ModItemTags.ESSENCE_LAVENDER)) return "lavender";
        if (stack.is(ModItemTags.ESSENCE_COCOA)) return "cocoa";
        if (stack.is(Items.APPLE)) return "apple";
        if (stack.is(Items.MELON_SLICE)) return "watermelon";
        if (stack.is(Items.SWEET_BERRIES)) return "berry";
        if (stack.is(Items.GLOW_BERRIES)) return "cherry";
        if (stack.is(Items.SUGAR)) return "vanilla";
        if (stack.is(Items.COCOA_BEANS)) return "coffee";
        if (stack.is(Items.BLAZE_POWDER)) return "cinnamon";
        if (stack.is(Items.PINK_PETALS)) return "rose";
        return null;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EssencePressBlockEntity(pos, state); }
    @Override public MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(EssencePressBlock::new); }
}
