package net.sorukoma.quasium.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.network.PacketAttachPipeBackpack;
import net.sorukoma.quasium.network.PacketDetachPipeBackpack;
import net.sorukoma.quasium.network.PacketOpenBackpackHookahGui;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientBackpackHookahKeyHandler {

    public static final KeyMapping OPEN_BACKPACK_HOOKAH_GUI_KEY =
            new KeyMapping(
                    "key.quasium.open_backpack_hookah_gui",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_B,
                    "key.categories.quasium"
            );

    public static final KeyMapping ATTACH_PIPE_BACKPACK_KEY =
            new KeyMapping(
                    "key.quasium.attach_pipe_backpack",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.quasium"
            );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BACKPACK_HOOKAH_GUI_KEY);
        event.register(ATTACH_PIPE_BACKPACK_KEY);
    }

    @EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            while (OPEN_BACKPACK_HOOKAH_GUI_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new PacketOpenBackpackHookahGui());
            }

            while (ATTACH_PIPE_BACKPACK_KEY.consumeClick()) {
                if (ClientHookahInputHandler.isConnectedToBackpack()) {
                    PacketDistributor.sendToServer(new PacketDetachPipeBackpack());
                } else {
                    PacketDistributor.sendToServer(new PacketAttachPipeBackpack());
                }
            }
        }
    }
}