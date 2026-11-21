package net.sorukoma.quasium.draft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;

public class FlaskChargerBlock extends BaseEntityBlock {
    public static final MapCodec<FlaskChargerBlock> CODEC = simpleCodec(FlaskChargerBlock::new);

    public FlaskChargerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlaskChargerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FlaskChargerBlockEntity charger) {
            HookahBlockEntity hookah = charger.hookah();
            player.displayClientMessage(hookah == null
                    ? Component.translatable("quasium.draft.charger.no_hookah")
                    : Component.translatable("quasium.draft.charger.charges", hookah.flaskCharges(), Config.FLASK_MAX_CHARGES.getAsInt()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
