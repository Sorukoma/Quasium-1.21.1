package net.sorukoma.quasium.golem;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.UUID;

final class ProgramClaims {
    private static final String PROXY_NAME = "quasium_golem";

    private ProgramClaims() {
    }

    static boolean mayBreak(ClayGolemEntity golem, BlockPos pos, BlockState state) {
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(golem.level(), pos, state, proxy(golem))).isCanceled();
    }

    static boolean mayReach(ClayGolemEntity golem, BlockPos pos) {
        return golem.level().mayInteract(proxy(golem), pos);
    }

    static Player hand(ClayGolemEntity golem, BlockPos pos) {
        Player hand = proxy(golem);
        hand.moveTo(golem.getX(), golem.getY(), golem.getZ());
        hand.lookAt(EntityAnchorArgument.Anchor.FEET, Vec3.atCenterOf(pos));
        return hand;
    }

    private static Player proxy(ClayGolemEntity golem) {
        ServerLevel level = (ServerLevel) golem.level();
        UUID owner = golem.ownerId();
        return owner == null ? FakePlayerFactory.getMinecraft(level) : FakePlayerFactory.get(level, new GameProfile(owner, PROXY_NAME));
    }
}
