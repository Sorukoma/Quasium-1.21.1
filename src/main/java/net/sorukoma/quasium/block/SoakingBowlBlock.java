package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sorukoma.quasium.block.entity.SoakingBowlBlockEntity;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import org.jetbrains.annotations.Nullable;

public class SoakingBowlBlock extends BaseEntityBlock {

    public SoakingBowlBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                                Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof SoakingBowlBlockEntity bowlEntity)) return ItemInteractionResult.FAIL;

        // Загрузка ферментированных листьев
        boolean isFermentedLeaves = (heldItem.getItem() == ModItems.TOBACCO_LEAVES.get() ||
                                   heldItem.getItem() == ModItems.MAKHORKA_LEAVES.get()) &&
                                  heldItem.has(ModDataComponents.FERMENTED.get());
        if (isFermentedLeaves) {
            ItemStack slotItem = bowlEntity.getItemHandler().getStackInSlot(0);
            if (slotItem.isEmpty()) {
                bowlEntity.getItemHandler().setStackInSlot(0, heldItem.copyWithCount(1));
                if (!player.isCreative()) heldItem.shrink(1);
                return ItemInteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.translatable("block.quasium.soaking_bowl.leaves_busy"), true);
                return ItemInteractionResult.CONSUME;
            }
        }

        // Загрузка зелья
        if (heldItem.getItem() == Items.POTION) {
            ItemStack potionSlot = bowlEntity.getItemHandler().getStackInSlot(1);
            if (potionSlot.isEmpty()) {
                bowlEntity.getItemHandler().setStackInSlot(1, heldItem.copyWithCount(1));
                if (!player.isCreative()) {
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
                    heldItem.shrink(1);
                }
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.added_potion", "пotion"), true);
                return ItemInteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.translatable("block.quasium.soaking_bowl.potion_busy"), true);
                return ItemInteractionResult.CONSUME;
            }
        }

        // Добавление мелассы (только до замеса, когда есть листья и зелье)
        if (heldItem.is(ModItems.MOLASSES_BOTTLE.get())) {
            int molassesMass = heldItem.getOrDefault(ModDataComponents.MOLASSES_MASS.get(), 5);
            if (molassesMass > 0) {
                // Проверяем, можно ли добавить мелассу (есть листья и зелье, но замес ещё не запущен)
                boolean hasLeavesAndPotion = !bowlEntity.getItemHandler().getStackInSlot(0).isEmpty() &&
                                            !bowlEntity.getItemHandler().getStackInSlot(1).isEmpty();
                boolean canAddMolasses = hasLeavesAndPotion && bowlEntity.getResultItem().isEmpty() && !bowlEntity.isSoaking()
                        && bowlEntity.getMolassesAmount() < 5; // не расходуем бутылку, если миска уже полна
                
                if (canAddMolasses) {
                    // Добавляем только 1 единицу за клик
                    bowlEntity.addMolasses(1);

                    // Удаляем 1 единицу из бутылки
                    int newMass = molassesMass - 1;
                    if (!player.isCreative() && newMass <= 0) {
                        // Бутылка пуста — возвращаем пустую бутылку и убираем текущую
                        player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
                        heldItem.shrink(1);
                    } else {
                        // Уменьшаем массу
                        if (!player.isCreative()) {
                            heldItem.set(ModDataComponents.MOLASSES_MASS.get(), newMass);
                        }
                    }

                    // Обновляем тултипы после изменения мелассы
                    bowlEntity.updateTooltip();

                    player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.added_molasses", 1), true);
                    return ItemInteractionResult.CONSUME;
                } else {
                    player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.no_tobacco"), true);
                    return ItemInteractionResult.CONSUME;
                }
            }
        }

        // Проверка на палочку для запуска процесса смешивания или показа прогресса
        if (heldItem.is(Items.STICK)) {
            ItemStack leavesSlot = bowlEntity.getItemHandler().getStackInSlot(0);
            boolean hasLeaves = !leavesSlot.isEmpty();
            boolean hasPotion = !bowlEntity.getItemHandler().getStackInSlot(1).isEmpty();
            boolean isSoaking = bowlEntity.isSoaking();
            
            if (isSoaking) {
                // Показываем прогресс замеса
                player.displayClientMessage(bowlEntity.getSoakingStatusMessage(), true);
                return ItemInteractionResult.CONSUME;
            } else if (hasLeaves && hasPotion) {
                // Запускаем замес
                boolean isMakhorka = leavesSlot.getItem() == ModItems.MAKHORKA_LEAVES.get();
                bowlEntity.startSoaking(isMakhorka, player);
                return ItemInteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.not_ready"), true);
                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof SoakingBowlBlockEntity bowlEntity)) return InteractionResult.FAIL;

        // Извлечение ванильной миской (работает всегда, когда есть результат)
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Items.BOWL)) {
            ItemStack resultSlot = bowlEntity.getResultItem();
            if (!resultSlot.isEmpty()) {
                player.getInventory().placeItemBackInInventory(resultSlot.copy());
                bowlEntity.clearResult();
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof SoakingBowlBlockEntity bowlEntity) {
                for (int i = 0; i < bowlEntity.getItemHandler().getSlots(); i++) {
                    ItemStack stack = bowlEntity.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
                ItemStack result = bowlEntity.getResultItem();
                if (!result.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), result);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoakingBowlBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.SOAKING_BOWL_BE.get(), SoakingBowlBlockEntity::tick);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SoakingBowlBlock::new);
    }
}