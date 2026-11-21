package net.sorukoma.quasium.magic.page;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class PageTargets {
    private PageTargets() {
    }

    public static boolean isAlly(@Nullable ServerPlayer caster, LivingEntity entity) {
        if (caster == null) return false;
        if (entity == caster) return true;
        if (entity instanceof OwnableEntity pet && caster.getUUID().equals(pet.getOwnerUUID())) return true;
        if (entity instanceof AbstractVillager) return true;
        if (entity instanceof Player player) return !caster.canHarmPlayer(player);
        return caster.isAlliedTo(entity);
    }

    public static boolean accepts(PageTarget target, @Nullable ServerPlayer caster, LivingEntity entity) {
        return switch (target) {
            case FOE -> entity != caster && !isAlly(caster, entity);
            case ALLY -> isAlly(caster, entity);
            case ANY -> entity != caster;
            case SUPPORT -> isAlly(caster, entity) || entity.getType().is(EntityTypeTags.UNDEAD);
            case WORLD -> false;
        };
    }
}
