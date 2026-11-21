package net.sorukoma.quasium.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.entity.SeatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class SeatedSmokerMixin extends LivingEntity {
    protected SeatedSmokerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void quasium$standUpByJump(CallbackInfoReturnable<Boolean> stop) {
        if (SeatEntity.keepsSmoker((Player) (Object) this)) stop.setReturnValue(jumping);
    }
}
