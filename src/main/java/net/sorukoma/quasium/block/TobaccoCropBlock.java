package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public abstract class TobaccoCropBlock extends CropBlock {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);
    private static final int SHEARED_AGE = 3;

    /**
     * Шанс дропа дополнительных семян при стрижке ножницами. 0.0 = нет дропа, 0.5 = 50%.
     * Override в subclass для настройки. Ищите MAKHORKA_SHEAR_SEED_DROP_CHANCE в RegularTobaccoCropBlock.
     */
    protected float getShearSeedDropChance() {
        return 0.0F;
    }

    /** Предмет, дропающийся при стрижке (в дополнение к листьям). null = нет дропа. */
    protected net.minecraft.world.item.Item getShearSeedDropItem() {
        return null;
    }

    public TobaccoCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 4;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    protected abstract net.minecraft.world.item.Item getSeedsItem();

    protected abstract net.minecraft.world.item.Item getLeavesItem();

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (state.getValue(AGE) == getMaxAge() && heldItem.is(Items.SHEARS)) {
            if (!level.isClientSide) {
                int count = 1;
                if (level.random.nextFloat() < 0.1F) {
                    count++;
                }
                Block.popResourceFromFace(level, pos, hitResult.getDirection(), new ItemStack(getLeavesItem(), count));

                float seedChance = getShearSeedDropChance();
                net.minecraft.world.item.Item seedDropItem = getShearSeedDropItem();
                if (seedChance > 0.0F && seedDropItem != null && level.random.nextFloat() < seedChance) {
                    Block.popResourceFromFace(level, pos, hitResult.getDirection(), new ItemStack(seedDropItem, 1));
                }

                heldItem.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);

                level.setBlockAndUpdate(pos, state.setValue(AGE, SHEARED_AGE));
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(heldItem, state, level, pos, player, hand, hitResult);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        int age = state.getValue(AGE);
        int count = age == getMaxAge() ? 2 : 1;
        return List.of(new ItemStack(getSeedsItem(), count));
    }
}