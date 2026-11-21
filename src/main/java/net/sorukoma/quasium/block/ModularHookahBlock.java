package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.block.entity.ModularHookahBlockEntity;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.hookah.HookahBulbVariant;
import net.sorukoma.quasium.item.HookahBulbItem;
import net.sorukoma.quasium.item.HookahShaftItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.component.ModDataComponents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Модульный кальян. Ставится колбой (этот блок), затем поэтапно собирается:
 *   колба → шахта (shift+ПКМ шахтой) → чаша (shift+ПКМ чашей) → каллауд (shift+ПКМ каллаудом).
 * Диффузор накручивается на шахту, если взять шахту/диффузор во вторую руку и нажать ПКМ.
 * Вода заливается ведром, пока не установлена шахта (одно ведро = 1000 мб).
 *
 * Механика курения полностью унаследована от базового кальяна (чаша/каллауд/жар/кайф).
 */
public class ModularHookahBlock extends BaseEntityBlock {
    public static final MapCodec<ModularHookahBlock> CODEC = simpleCodec(ModularHookahBlock::new);

    public ModularHookahBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Сколько мб добавляет одна бутылочка воды (пузырёк). */
    public static final int WATER_BOTTLE_MB = 250;

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
        var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(net.minecraft.world.item.alchemy.Potions.WATER);
    }

    public ModularHookahBlock() {
        this(BlockBehaviour.Properties.ofFullCopy(Blocks.BREWING_STAND)
                .sound(SoundType.GLASS)
                .noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        // Базовая колба рендерится через JSON-модель blockstate; остальные части — через BER.
        return RenderShape.MODEL;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    private static final VoxelShape BULB = Shapes.or(Block.box(4, 0, 4, 12, 6, 12), Block.box(5, 6, 5, 11, 8, 11),
            Block.box(6.5, 8, 6.5, 9.5, 10, 9.5));
    private static final VoxelShape WITH_SHAFT = Shapes.or(BULB, Block.box(7, 5, 7, 9, 14, 9));
    private static final VoxelShape WITH_BOWL = Shapes.or(WITH_SHAFT, Block.box(7, 14, 7, 9, 15, 9), Block.box(6, 15, 6, 10, 17, 10));
    private static final VoxelShape WITH_CALLOUD = Shapes.or(WITH_BOWL, Block.box(6, 17, 6, 10, 19.5, 10));

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof ModularHookahBlockEntity hookah) || !hookah.hasShaft()) return BULB;
        if (hookah.getItemHandler().getStackInSlot(1).isEmpty()) return WITH_SHAFT;
        return hookah.getItemHandler().getStackInSlot(0).isEmpty() ? WITH_BOWL : WITH_CALLOUD;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BULB;
    }

    // Не затемнять окружение: пропускать небесный свет и не давать полное AO-затенение.
    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ModularHookahBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pType) {
        if (pLevel.isClientSide()) return null;
        return createTickerHelper(pType, ModBlockEntities.MODULAR_HOOKAH_BE.get(), HookahBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.EMPTY_SMOKE_JAR.get()) || stack.is(ModItems.KAIFOMETER.get()) || stack.is(ModItems.HOOKAH_PIPE.get())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ModularHookahBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return handleInteraction(be, stack, level, pos, player, hand)
                ? ItemInteractionResult.CONSUME
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Общая логика взаимодействия с модульным кальяном — вызывается и из {@link #useItemOn}
     * (обычный ПКМ без shift), и из серверного обработчика пакета
     * {@code PacketModularHookahInteract} (shift+ПКМ с предметом, который ванилла не
     * доводит до useItemOn). Возвращает true, если взаимодействие обработано.
     */
    public static boolean handleInteraction(ModularHookahBlockEntity be, ItemStack stack, Level level,
                                            BlockPos pos, Player player, InteractionHand hand) {
        // Трубка — прикрепление/затяжка (управляется клиентскими пакетами, как у базового кальяна).
        if (stack.is(ModItems.HOOKAH_PIPE.get())) {
            return true;
        }

        // Заливка воды ведром — только пока не установлена шахта.
        if (stack.is(Items.WATER_BUCKET.asItem())) {
            if (be.hasShaft()) {
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.shaft_blocks_water"), true);
                return true;
            }
            if (be.getWaterMb() >= ModularHookahBlockEntity.MAX_WATER_MB) {
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.bulb_full"), true);
                return true;
            }
            be.setWaterMb(ModularHookahBlockEntity.MAX_WATER_MB);
            if (!player.isCreative()) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.water_filled",
                    be.getWaterMb()), true);
            return true;
        }

        // Слив воды пустым ведром.
        if (stack.is(Items.BUCKET.asItem()) && !be.hasShaft() && be.getWaterMb() > 0) {
            be.setWaterMb(0);
            if (!player.isCreative()) {
                stack.shrink(1);
                player.getInventory().add(new ItemStack(Items.WATER_BUCKET));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }

        // Заливка воды бутылочкой с водой (пузырёк) — по WATER_BOTTLE_MB за раз.
        if (isWaterBottle(stack)) {
            if (be.hasShaft()) {
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.shaft_blocks_water"), true);
                return true;
            }
            if (be.getWaterMb() >= ModularHookahBlockEntity.MAX_WATER_MB) {
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.bulb_full"), true);
                return true;
            }
            be.setWaterMb(be.getWaterMb() + WATER_BOTTLE_MB);
            if (!player.isCreative()) {
                stack.shrink(1);
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.water_filled",
                    be.getWaterMb()), true);
            return true;
        }

        // Диффузор в любой руке → накрутить на шахту.
        boolean holdingDiffuser = stack.is(ModItems.HOOKAH_DIFFUSER.get())
                || player.getOffhandItem().is(ModItems.HOOKAH_DIFFUSER.get())
                || player.getMainHandItem().is(ModItems.HOOKAH_DIFFUSER.get());
        if (holdingDiffuser) {
            if (!be.hasShaft()) {
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.need_shaft"), true);
                return true;
            }
            if (be.hasScrewedDiffuser()) {
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.diffuser_present"), true);
                return true;
            }
            be.setScrewedDiffuser(true);
            if (!player.isCreative()) {
                if (player.getMainHandItem().is(ModItems.HOOKAH_DIFFUSER.get())) {
                    player.getMainHandItem().shrink(1);
                } else if (player.getOffhandItem().is(ModItems.HOOKAH_DIFFUSER.get())) {
                    player.getOffhandItem().shrink(1);
                }
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
            player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.diffuser_added"), true);
            return true;
        }

        // Сборка по shift+ПКМ.
        if (player.isShiftKeyDown()) {
            // Шахта: колба уже стоит → закрепить шахту.
            if (stack.getItem() instanceof HookahShaftItem shaftItem) {
                if (be.hasShaft()) {
                    player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.shaft_present"), true);
                    return true;
                }
                be.setShaft(shaftItem.getVariant());
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.9f, 1.0f);
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.shaft_added"), true);
                return true;
            }

            // Чаша: нужна установленная шахта.
            if (stack.is(ModItems.HOOKAH_BOWL.get())) {
                if (!be.hasShaft()) {
                    player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.need_shaft"), true);
                    return true;
                }
                ItemStack existing = be.getItemHandler().getStackInSlot(1);
                if (!existing.isEmpty()) {
                    player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.bowl_present"), true);
                    return true;
                }
                be.getItemHandler().insertItem(1, stack.copyWithCount(1), false);
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.9f, 0.9f);
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.bowl_added"), true);
                return true;
            }

            // Каллауд: нужна установленная чаша.
            if (stack.getItem() instanceof net.sorukoma.quasium.item.CalloudItem) {
                if (be.getItemHandler().getStackInSlot(1).isEmpty()) {
                    player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.need_bowl"), true);
                    return true;
                }
                if (!be.getItemHandler().getStackInSlot(0).isEmpty()) {
                    player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.calloud_present"), true);
                    return true;
                }
                be.getItemHandler().insertItem(0, stack.copyWithCount(1), false);
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.9f, 0.8f);
                player.displayClientMessage(Component.translatable("quasium.message.modular_hookah.calloud_added"), true);
                return true;
            }
        }

        // ПКМ без предмета/без shift, когда собрано — открыть GUI (углей/чаши/аксессуаров).
        if (!player.isShiftKeyDown() && be.hasShaft() && player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, be, pos);
            return true;
        }

        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ModularHookahBlockEntity be)) {
            return InteractionResult.PASS;
        }
        // Shift+ПКМ без предмета — открепление трубки (клиентский пакет), иначе открыть GUI.
        if (player.isShiftKeyDown()) {
            return InteractionResult.CONSUME;
        }
        if (be.hasShaft()) {
            open((ServerPlayer) player, be, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static void open(ServerPlayer player, ModularHookahBlockEntity be, BlockPos pos) {
        QuasiumModAdvancements.HOOKAH_OPENED.get().trigger(player);
        player.openMenu(be, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof ModularHookahBlockEntity be) {
                // Останавливаем и удаляем прикреплённые трубки.
                List<HookahConnectionEntity> attached = level.getEntitiesOfClass(
                        HookahConnectionEntity.class,
                        new AABB(pos).inflate(HookahConnectionEntity.PIPE_DETACH_DISTANCE_SQ + 1.0),
                        c -> c.getHookahBlockPos() != null && c.getHookahBlockPos().equals(pos));
                for (HookahConnectionEntity c : attached) {
                    Player p = level.getPlayerByUUID(c.getAttachedPlayerUUID());
                    if (p != null) {
                        c.stopPuffing(be, p);
                        p.displayClientMessage(Component.translatable("quasium.message.pipe_detached_broken"), true);
                    }
                    c.discard();
                }
                // Дропаем чашу/каллауд/аксессуары.
                be.drops();
                // Дропаем шахту как отдельный предмет варианта.
                if (be.hasShaft() && be.getShaftVariant() != null) {
                    ItemStack shaftDrop = ModItems.shaftItemFor(be.getShaftVariant());
                    if (!shaftDrop.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, shaftDrop);
                    }
                }
                if (be.hasScrewedDiffuser()) {
                    net.minecraft.world.Containers.dropItemStack(level,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            new ItemStack(ModItems.HOOKAH_DIFFUSER.get()));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * При установке блока переносим вариант колбы с предмета в BlockEntity, чтобы
     * рендерилась колба нужного цвета.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ModularHookahBlockEntity be) {
            String variant = stack.get(ModDataComponents.HOOKAH_BULB_VARIANT.get());
            if (variant == null && stack.getItem() instanceof HookahBulbItem bulbItem) {
                variant = bulbItem.getVariant().getName();
            }
            if (variant != null) {
                be.setBulbVariant(HookahBulbVariant.byName(variant));
            }
        }
    }
}
