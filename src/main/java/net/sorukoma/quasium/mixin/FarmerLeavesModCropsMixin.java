package net.sorukoma.quasium.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.ai.behavior.UseBonemeal;
import net.sorukoma.quasium.QuasiumMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({HarvestFarmland.class, UseBonemeal.class})
public abstract class FarmerLeavesModCropsMixin {
    @Inject(method = "validPos", at = @At("HEAD"), cancellable = true)
    private void quasium$leaveModCrops(BlockPos pos, ServerLevel level, CallbackInfoReturnable<Boolean> valid) {
        if (BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getNamespace().equals(QuasiumMod.MOD_ID)) valid.setReturnValue(false);
    }
}
