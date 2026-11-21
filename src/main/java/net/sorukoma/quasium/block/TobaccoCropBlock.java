package net.sorukoma.quasium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;
import net.sorukoma.quasium.block.entity.TobaccoCropBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.selection.genetics.Lineage;
import net.sorukoma.quasium.selection.quality.Aphids;
import net.sorukoma.quasium.selection.quality.LeafQuality;

import java.util.List;

public abstract class TobaccoCropBlock extends CropBlock implements EntityBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);
    public static final BooleanProperty WATERED = BooleanProperty.create("watered");
    public static final IntegerProperty MUTATION = IntegerProperty.create("mutation", 0, 6);
    public static final BooleanProperty APHIDS = BooleanProperty.create("aphids");
    private static final int SHEARED_AGE = 3;
    private static final VoxelShape[] SHAPE_BY_AGE = {
            Block.box(0, 0, 0, 16, 3, 16), Block.box(0, 0, 0, 16, 5, 16), Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 0, 0, 16, 11, 16), Block.box(0, 0, 0, 16, 14, 16)};

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

    public TobaccoCropBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(AGE, 0).setValue(WATERED, false)
                .setValue(MUTATION, 0).setValue(APHIDS, false));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[getAge(state)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, WATERED, MUTATION, APHIDS);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TobaccoCropBlockEntity(pos, state);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.getBlock() instanceof FarmBlock
                || state.is(TobaccoVarieties.soil(getVariety()));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        TriState soilDecision = belowState.canSustainPlant(level, below, Direction.UP, state);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        return mayPlaceOn(belowState, level, below);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity crop) {
            crop.initWild(random);
            state = Aphids.dailyRoll(level, pos, state, crop, random);
        }
        BlockState before = state;
        if (random.nextInt(100) < growthPct(state, level)) {
            int attempts = state.getValue(MUTATION) == mutationCode("fast_growth") ? 2 : 1;
            if (state.getValue(WATERED) && random.nextFloat() < 0.2f) attempts++;
            for (int i = 0; i < attempts && !isMaxAge(state); i++) state = growStep(state, level, pos, random);
        }
        noteRipening(level, pos, before, state);
        if (state.getValue(WATERED) && random.nextFloat() < 0.2f) {
            level.setBlock(pos, state.setValue(WATERED, false), 2);
        }
    }

    private BlockState growStep(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getRawBrightness(pos, 0) < 9) return state;
        float speed = getGrowthSpeed(state, level, pos);
        if (!CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (25.0F / speed) + 1) == 0)) return state;
        BlockState grown = state.setValue(AGE, getAge(state) + 1);
        level.setBlock(pos, grown, 2);
        CommonHooks.fireCropGrowPost(level, pos, state);
        return grown;
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        if (level.random.nextInt(100) >= growthPct(state, level)) return;
        BlockState grown = state.setValue(AGE, Math.min(getMaxAge(), getAge(state) + getBonemealAgeIncrease(level)));
        level.setBlock(pos, grown, 2);
        noteRipening(level, pos, state, grown);
    }

    public void growStage(Level level, BlockPos pos, BlockState state) {
        BlockState grown = state.setValue(AGE, getAge(state) + 1);
        level.setBlockAndUpdate(pos, grown);
        noteRipening(level, pos, state, grown);
    }

    public int growthPct(BlockState state, Level level) {
        return Aphids.growthPct(state.getValue(APHIDS), TobaccoVarieties.growthPct(getVariety(), level));
    }

    private void noteRipening(Level level, BlockPos pos, BlockState before, BlockState after) {
        if (isMaxAge(before) || !isMaxAge(after)) return;
        if (level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity crop) crop.markMature(level.getGameTime());
    }

    public boolean isExactMature(Level level, BlockPos pos) {
        return isMaxAge(level.getBlockState(pos)) && level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity crop
                && crop.ripeWithin(level.getGameTime(), SelectionConfig.EXACT_MATURE_WINDOW_SECONDS.get() * 20L);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(APHIDS) || random.nextInt(3) != 0) return;
        level.addParticle(ModParticles.APHID.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 0.15 + random.nextDouble() * 0.5, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
    }

    public String getVariety() {
        return TobaccoVarieties.DIKAR;
    }

    protected abstract Item getSeedsItem();

    protected abstract Item getLeavesItem();

    @Override
    protected ItemLike getBaseSeedId() {
        return getSeedsItem();
    }

    protected int getShearYield(BlockState state, Level level, BlockPos pos) {
        int yield = level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity be
                ? be.getYieldStars()
                : TobaccoVarieties.yield(getVariety());
        if (state.getValue(MUTATION) == mutationCode("double_yield")) yield *= 2;
        return yield;
    }

    protected void applyLeafData(ItemStack leaves, BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity be) {
            leaves.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), be.getStrengthStars());
            leaves.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), be.getYieldStars());
            leaves.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), be.getQualityStars());
            Lineage.apply(leaves, be.lineage());
        } else {
            leaves.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), TobaccoVarieties.strength(getVariety()));
            leaves.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), TobaccoVarieties.yield(getVariety()));
            leaves.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), TobaccoVarieties.quality(getVariety()));
        }
        leaves.set(SelectionRegistry.LEAF_QUALITY.get(), LeafQuality.atHarvest(level, pos, state));
        String mutation = mutationName(state.getValue(MUTATION));
        if (!mutation.isEmpty()) leaves.set(ModDataComponents.TOBACCO_MUTATION.get(), mutation);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
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
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (Aphids.fumigate(heldItem, state, level, pos, player, hand)) return ItemInteractionResult.sidedSuccess(level.isClientSide);

        if (state.getValue(AGE) == getMaxAge() && heldItem.is(Items.SHEARS)) {
            if (!level.isClientSide) {
                shearLeaves(state, level, pos).forEach(drop -> Block.popResourceFromFace(level, pos, hitResult.getDirection(), drop));
                heldItem.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(heldItem, state, level, pos, player, hand, hitResult);
    }

    public List<ItemStack> shearLeaves(BlockState state, Level level, BlockPos pos) {
        int count = getShearYield(state, level, pos);
        if (level.random.nextFloat() < 0.1F) count++;
        ItemStack leaves = new ItemStack(getLeavesItem(), count);
        applyLeafData(leaves, state, level, pos);
        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlockAndUpdate(pos, state.setValue(AGE, SHEARED_AGE));
        if (level.getBlockEntity(pos) instanceof TobaccoCropBlockEntity crop) crop.forgetMaturity();
        return List.of(leaves);
    }

    public ItemStack seedsWithGenes(BlockState state, TobaccoCropBlockEntity crop, int count) {
        ItemStack seeds = new ItemStack(getSeedsItem(), count);
        seeds.set(ModDataComponents.TOBACCO_VARIETY.get(), getVariety());
        seeds.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), crop.getStrengthStars());
        seeds.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), crop.getYieldStars());
        seeds.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), crop.getQualityStars());
        String mutation = mutationName(state.getValue(MUTATION));
        if (!mutation.isEmpty()) seeds.set(ModDataComponents.TOBACCO_MUTATION.get(), mutation);
        Lineage.apply(seeds, crop.lineage());
        return seeds;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        int count = state.getValue(AGE) == getMaxAge() ? 2 : 1;
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof TobaccoCropBlockEntity crop
                && Lineage.isHybrid(crop.lineage())) {
            return List.of(seedsWithGenes(state, crop, count));
        }
        return List.of(new ItemStack(getSeedsItem(), count));
    }
}
