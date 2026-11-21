package net.sorukoma.quasium.client.tetrad;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.page.PageRegistry;
import net.sorukoma.quasium.magic.page.SpellPage;
import net.sorukoma.quasium.magic.page.SpellTetradItem;
import net.sorukoma.quasium.magic.page.TetradContents;
import net.sorukoma.quasium.magic.page.TetradPackets;
import net.sorukoma.quasium.magic.page.Tetrads;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class TetradKeys {
    public static final KeyMapping FLIP = new KeyMapping("key.quasium.page_flip", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.quasium");
    private static final int HOLD_TICKS = 6;
    private static int heldTicks;

    private TetradKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(FLIP);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean clicked = false;
        while (FLIP.consumeClick()) clicked = true;
        if (player == null || minecraft.screen != null) {
            heldTicks = 0;
            return;
        }
        if (FLIP.isDown()) {
            if (++heldTicks == HOLD_TICKS) openWheel(minecraft, player);
            return;
        }
        boolean tapped = heldTicks > 0 && heldTicks < HOLD_TICKS || heldTicks == 0 && clicked;
        heldTicks = 0;
        if (tapped && !Tetrads.find(player).isEmpty()) TetradPackets.sendFlip(player.isShiftKeyDown() ? -1 : 1);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() || !(event.getItemStack().getItem() instanceof SpellTetradItem)) return;
        Minecraft.getInstance().setScreen(new TetradScreen(event.getHand(), event.getItemStack().getHoverName()));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(PageRegistry.TORN_PAGE.get())) return;
        SpellPage page = stack.getOrDefault(PageRegistry.PAGE.get(), SpellPage.EMPTY);
        List<Component> tooltip = event.getToolTip();
        if (page.isEmpty()) {
            tooltip.add(Component.translatable("quasium.tetrad.torn.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(PageList.label(page).copy().withStyle(ChatFormatting.YELLOW));
        tooltip.add(PageList.composition(page).copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("quasium.tetrad.torn.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static boolean flipHeld() {
        InputConstants.Key key = FLIP.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        return key.getType() == InputConstants.Type.KEYSYM && InputConstants.isKeyDown(window, key.getValue());
    }

    private static void openWheel(Minecraft minecraft, LocalPlayer player) {
        ItemStack tetrad = Tetrads.find(player);
        if (tetrad.isEmpty()) return;
        TetradContents contents = SpellTetradItem.contents(tetrad);
        if (contents.pages().stream().allMatch(SpellPage::isEmpty)) return;
        minecraft.setScreen(new PageWheelScreen(contents, true));
    }
}
