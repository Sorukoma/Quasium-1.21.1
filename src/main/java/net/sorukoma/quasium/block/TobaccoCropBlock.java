package net.sorukoma.quasium.block;

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

import net.sorukoma.quasium.block.entity.TobaccoCropBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;

public abstract class TobaccoCropBlock extends CropBlock implements net.minecraft.world.level.block.EntityBlock {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERED = net.minecraft.world.level.block.state.properties.BooleanProperty.create("watered");
    public static final IntegerProperty MUTATION = IntegerProperty.create("mutation", 0, 6);
    private static final int SHEARED_AGE = 3;

    public static int mutationCode(String mutation) {
        return switch (mutation) {
            case "double_yield" -> 1;
            case "fast_growth" -> 2;
            case "hardy" -> 3;
            case "concentrated" -> 4;
            case "premium_quality" -> 5;
            case "toxic" -> 6;
            default -> 0;
        };
    }

    public static String mutationName(int mutation) {
        return switch (mutation) {
            case 1 -> "double_yield";
            case 2 -> "fast_growth";
            case 3 -> "hardy";
            case 4 -> "concentrated";
            case 5 -> "premium_quality";
            case 6 -> "toxic";
            default -> "";
        };
    }

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
        registerDefaultState(defaultBlockState()
                .setValue(AGE, 0).setValue(WATERED, false)
                .setValue(MUTATION, 0));
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
        builder.add(AGE, WATERED, MUTATION);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TobaccoCropBlockEntity(pos, state);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.tags.BlockTags.DIRT)
                || state.is(net.minecraft.tags.BlockTags.SAND)
                || state.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
    }

    /**
     * Без проверки освещённости: дикие кусты ставятся worldgen-фичей, а на этапе генерации
     * чанка свет ещё не рассчитан, поэтому светозависимый CropBlock.canSurvive отсёк бы их все.
     * Ограничение по свету остаётся только в randomTick (рост), но не в выживании.
     */
    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        net.neoforged.neoforge.common.util.TriState soilDecision =
                belowState.canSustainPlant(level, below, net.minecraft.core.Direction.UP, state);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        return mayPlaceOn(belowState, level, below);
    }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity be && !be.isInitialized()) {
            be.initWild(random);
        }
        int attempts = state.getValue(MUTATION) == mutationCode("fast_growth") ? 2 : 1;
        if (state.getValue(WATERED) && random.nextFloat() < 0.2f) attempts++;
        for (int i = 0; i < attempts; i++) {
            super.randomTick(state, level, pos, random);
            state = level.getBlockState(pos);
            if (state.getValue(AGE) == getMaxAge()) break;
        }
        if (state.getValue(WATERED) && random.nextFloat() < 0.2f) {
            level.setBlock(pos, state.setValue(WATERED, false), 2);
        }
    }

    public String getVariety() {
        return "dikar";
    }

    protected abstract net.minecraft.world.item.Item getSeedsItem();

    protected abstract net.minecraft.world.item.Item getLeavesItem();

    protected int getShearYield(BlockState state, Level level, BlockPos pos) {
        int yield = level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity be
                ? be.getYieldStars()
                : net.sorukoma.quasium.item.TobaccoVarieties.yield(getVariety());
        if (state.getValue(MUTATION) == mutationCode("double_yield")) yield *= 2;
        return yield;
    }

    protected void applyLeafData(ItemStack leaves, BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity be) {
            leaves.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), be.getStrengthStars());
            leaves.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), be.getYieldStars());
            leaves.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), be.getQualityStars());
        } else {
            leaves.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), net.sorukoma.quasium.item.TobaccoVarieties.strength(getVariety()));
            leaves.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), net.sorukoma.quasium.item.TobaccoVarieties.yield(getVariety()));
            leaves.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), net.sorukoma.quasium.item.TobaccoVarieties.quality(getVariety()));
        }
        String mutation = mutationName(state.getValue(MUTATION));
        if (!mutation.isEmpty()) leaves.set(ModDataComponents.TOBACCO_MUTATION.get(), mutation);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (heldItem.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide && state.getValue(AGE) < getMaxAge()) {
                level.setBlock(pos, state.setValue(WATERED, true), 2);
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.7f, 1.1f);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.getValue(AGE) == getMaxAge() && heldItem.is(Items.SHEARS)) {
            if (!level.isClientSide) {
                int count = getShearYield(state, level, pos);
                if (level.random.nextFloat() < 0.1F) {
                    count++;
                }
                ItemStack leaves = new ItemStack(getLeavesItem(), count);
                applyLeafData(leaves, state, level, pos);
                Block.popResourceFromFace(level, pos, hitResult.getDirection(), leaves);

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