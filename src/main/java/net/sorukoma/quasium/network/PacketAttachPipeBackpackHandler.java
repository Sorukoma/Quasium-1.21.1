package net.sorukoma.quasium.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.cart.HookahMinecart;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.PacketSyncHookahConnection;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Optional;

public class PacketAttachPipeBackpackHandler {
    public static void handle(final PacketAttachPipeBackpack payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean hasBackpackHookah = CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BACKPACK_HOOKAH.get()).isPresent();
            if (!hasBackpackHookah) {
                player.sendSystemMessage(Component.translatable("quasium.message.no_backpack_hookah"));
                return;
            }
            boolean holdingPipe = player.getMainHandItem().is(ModItems.HOOKAH_PIPE.get()) ||
                    player.getOffhandItem().is(ModItems.HOOKAH_PIPE.get());
            if (!holdingPipe) {
                player.sendSystemMessage(Component.translatable("quasium.message.need_pipe_to_connect"));
                return;
            }

            // Detach from any standing hookah first
            List<HookahConnectionEntity> allPlayerConnections = player.level().getEntitiesOfClass(
                    HookahConnectionEntity.class,
                    new AABB(player.blockPosition()).inflate(64.0),
                    e -> e.getAttachedPlayerUUID() != null &&
                         e.getAttachedPlayerUUID().equals(player.getUUID())
            );
            for (HookahConnectionEntity oldConn : allPlayerConnections) {
                BlockEntity oldBe = player.level().getBlockEntity(oldConn.getHookahBlockPos());
                if (oldBe instanceof HookahBlockEntity oldHookahBe) {
                    oldConn.stopPuffing(oldHookahBe, player);
                }
                oldConn.discard();
            }
            if (!allPlayerConnections.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.empty()));
            }
            HookahMinecart.unhookAll(player);

            PacketDistributor.sendToPlayer(player, new PacketSyncBackpackPipeConnection(true));
            player.sendSystemMessage(Component.translatable("quasium.message.pipe_attached_backpack"));
        });
    }
}