package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.block.entity.SoakingBowlBlockEntity;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.SelfMixedTobaccoItem;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.selection.genetics.Genetics;
import net.sorukoma.quasium.selection.variety.SeedSoaking;
import org.jetbrains.annotations.Nullable;

public class SoakingBowlBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.join(box(3, 0, 3, 13, 4, 13), box(4, 1, 4, 12, 4, 12), BooleanOp.ONLY_FIRST);

    public SoakingBowlBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                                Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof SoakingBowlBlockEntity bowlEntity)) return ItemInteractionResult.FAIL;

        boolean isFermentedLeaves = TobaccoVarieties.isLeaf(heldItem)
                && heldItem.has(ModDataComponents.FERMENTED.get());

        if (isFermentedLeaves) {
            if (heldItem.getOrDefault(ModDataComponents.SPOILED.get(), false)) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.spoiled_leaves"), true);
                return ItemInteractionResult.CONSUME;
            }
            if (bowlEntity.getTobaccoGrams() >= 1) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.full"), true);
                return ItemInteractionResult.CONSUME;
            }
            bowlEntity.addLeaf(heldItem);
            if (!player.isCreative()) heldItem.shrink(1);
            player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.added_tobacco", 30), true);
            bowlEntity.setChanged();
            return ItemInteractionResult.CONSUME;
        }

        // Свежие (неферментированные) листья замешивать нельзя — только ферментированные.
        if (TobaccoVarieties.isLeaf(heldItem) && !heldItem.has(ModDataComponents.FERMENTED.get())) {
            player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.needs_fermented"), true);
            return ItemInteractionResult.CONSUME;
        }

        if (SeedSoaking.isCatalyst(heldItem)) {
            String outcome = catalystProblem(bowlEntity, heldItem, player);
            if (outcome == null) {
                boolean mix = SeedSoaking.sprout(heldItem, bowlEntity.getVariety(), Genetics.MIN_STARS).getItem() instanceof SelfMixedTobaccoItem;
                bowlEntity.setCatalyst(heldItem.copyWithCount(1));
                if (!player.isCreative()) heldItem.shrink(1);
                outcome = mix ? "mix_catalyst_added" : "catalyst_added";
            }
            player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl." + outcome), true);
            return ItemInteractionResult.CONSUME;
        }

        if ((heldItem.is(ModItems.MOLASSES_BOTTLE.get()) || heldItem.is(ModItems.ESSENCE_BOTTLE.get())) && !bowlEntity.getCatalyst().isEmpty()) {
            player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.catalyst_liquid"), true);
            return ItemInteractionResult.CONSUME;
        }

        if (heldItem.is(ModItems.MOLASSES_BOTTLE.get())) {
            int mass = 5;
            if (bowlEntity.getMolassesMb() + mass > 20) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.molasses_full"), true);
                return ItemInteractionResult.CONSUME;
            }
            bowlEntity.setMolassesMb(bowlEntity.getMolassesMb() + mass);
            if (!player.isCreative()) {
                heldItem.shrink(1);
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
            }
            player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.added_molasses", bowlEntity.getMolassesMb()), true);
            bowlEntity.setChanged();
            return ItemInteractionResult.CONSUME;
        }

        if (heldItem.is(ModItems.ESSENCE_BOTTLE.get())) {
            int amount = heldItem.getOrDefault(ModDataComponents.ESSENCE_AMOUNT.get(), 20);
            String type = heldItem.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "unknown");
            int transferred = Math.min(5, Math.min(amount, 20 - bowlEntity.getEssenceMb()));
            if (transferred <= 0) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.essence_full"), true);
                return ItemInteractionResult.CONSUME;
            }
            bowlEntity.addEssence(type, transferred);
            if (!player.isCreative()) {
                int remaining = amount - transferred;
                if (remaining <= 0) {
                    heldItem.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
                } else {
                    heldItem.set(ModDataComponents.ESSENCE_AMOUNT.get(), remaining);
                }
            }
            player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.added_essence", type, bowlEntity.getEssenceMb()), true);
            return ItemInteractionResult.CONSUME;
        }

        if (heldItem.is(Items.STICK)) {
            if (!bowlEntity.canStartSoaking() && !bowlEntity.isSoaking()) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.bad_balance"), true);
            } else {
                bowlEntity.stir();
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.stirred"), true);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (heldItem.is(Items.BOWL)) {
            if (!bowlEntity.isReady()) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.not_ready"), true);
                return ItemInteractionResult.CONSUME;
            }
            ItemStack result = bowlEntity.takeResult();
            if (!result.isEmpty()) {
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.BOWL));
                }
                player.getInventory().placeItemBackInInventory(result);
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.transferred"), true);
            }
            return ItemInteractionResult.CONSUME;
        }

        if (heldItem.is(ModItems.HOOKAH_BOWL.get())) {
            player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.use_tobacco_pack"), true);
            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    private static String catalystProblem(SoakingBowlBlockEntity bowl, ItemStack catalyst, Player player) {
        if (bowl.getTobaccoGrams() < 1) return "catalyst_no_leaves";
        if (!bowl.getCatalyst().isEmpty()) return "catalyst_full";
        if (bowl.getMolassesMb() + bowl.getEssenceMb() > 0) return "catalyst_liquid";
        if (!SeedSoaking.knows(player, catalyst)) return "catalyst_unknown";
        return SeedSoaking.sprout(catalyst, bowl.getVariety(), Genetics.MIN_STARS).isEmpty() ? "catalyst_wrong" : null;
    }

    public static java.util.List<MobEffectInstance> createEssenceEffects(String flavor) {
        java.util.List<MobEffectInstance> effects = new java.util.ArrayList<>();
        for (String type : flavor.split("\\+")) {
            var effect = switch (type) {
                case "mint" -> MobEffects.MOVEMENT_SPEED;
                case "lemon", "lime", "orange", "grapefruit" -> MobEffects.DAMAGE_BOOST;
                case "berry" -> MobEffects.JUMP;
                case "lavender" -> MobEffects.REGENERATION;
                case "cocoa" -> MobEffects.DAMAGE_RESISTANCE;
                case "apple" -> MobEffects.HEALTH_BOOST;
                case "watermelon" -> MobEffects.WATER_BREATHING;
                case "grape" -> MobEffects.LUCK;
                case "cherry" -> MobEffects.NIGHT_VISION;
                case "vanilla" -> MobEffects.SLOW_FALLING;
                case "coffee" -> MobEffects.DIG_SPEED;
                case "cinnamon" -> MobEffects.DAMAGE_BOOST;
                case "coconut" -> MobEffects.DAMAGE_RESISTANCE;
                case "rose" -> MobEffects.REGENERATION;
                case "anise" -> MobEffects.MOVEMENT_SPEED;
                case "ice" -> MobEffects.FIRE_RESISTANCE;
                case "honey" -> MobEffects.ABSORPTION;
                case "nether" -> MobEffects.FIRE_RESISTANCE;
                default -> null;
            };
            if (effect != null) effects.add(new MobEffectInstance(effect, 200, 0));
        }
        return effects;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof SoakingBowlBlockEntity bowlEntity)) return InteractionResult.FAIL;

        int grams = bowlEntity.getTobaccoGrams();
        if (grams > 0) {
            float ratio = grams > 0 ? bowlEntity.getMolassesMb() / (float)grams : 0f;
            if (bowlEntity.isSoaking()) {
                int seconds = Math.max(0, (bowlEntity.getSoakingTotalTime() - bowlEntity.getSoakingProgress()) / 20);
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.soaking_progress", seconds), true);
            } else if (bowlEntity.isReady() && bowlEntity.isToxic()) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.status_toxic"), true);
            } else if (bowlEntity.isReady()) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.status_ideal"), true);
            } else if (!bowlEntity.getCatalyst().isEmpty()) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.status_needs_stirring"), true);
            } else if (ratio < 0.15f) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.status_dry"), true);
            } else if (ratio > 0.25f) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.status_wet"), true);
            } else if (bowlEntity.getEssenceMb() <= 0) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.status_no_essence"), true);
            } else if (bowlEntity.isToxic()) {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.status_toxic"), true);
            } else {
                player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.status_needs_stirring"), true);
            }
            return InteractionResult.CONSUME;
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
