package net.sorukoma.quasium.progression;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.network.ClaimedRewardsPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class BookRewards {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuasiumMod.MOD_ID);
    private static final Supplier<AttachmentType<List<String>>> CLAIMED = ATTACHMENTS.register("claimed_book_rewards",
            () -> AttachmentType.<List<String>>builder(() -> List.of()).serialize(Codec.STRING.listOf()).copyOnDeath().build());

    private BookRewards() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    public static void claim(ServerPlayer player, ResourceLocation articleId) {
        ProgressionArticle article = QuasiumMod.getProgressionArticleManager().getArticles().get(articleId);
        Item reward = article == null ? Items.AIR : rewardOf(article);
        if (reward == Items.AIR) return;
        if (!unlocked(player, article)) {
            player.sendSystemMessage(Component.translatable("quasium.progression.reward_locked"));
            return;
        }
        if (grant(player, articleId, reward)) {
            player.sendSystemMessage(Component.translatable("quasium.progression.reward_given", reward.getDefaultInstance().getHoverName()));
        } else {
            buyCopy(player, reward);
        }
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClaimedRewardsPacket(player.getData(CLAIMED)));
    }

    public static boolean grant(ServerPlayer player, ResourceLocation articleId, Item reward) {
        List<String> claimed = player.getData(CLAIMED);
        if (claimed.contains(articleId.toString())) return false;
        List<String> updated = new ArrayList<>(claimed);
        updated.add(articleId.toString());
        player.setData(CLAIMED, List.copyOf(updated));
        give(player, reward);
        return true;
    }

    public static boolean buyCopy(ServerPlayer player, Item reward) {
        int price = MindConfig.REWARD_COPY_INSIGHT.getAsInt();
        int insight = PlayerMagicData.getInsight(player);
        if (insight < price) {
            player.sendSystemMessage(Component.translatable("quasium.progression.reward_copy_short", price, insight));
            return false;
        }
        PlayerMagicData.addInsight(player, -price);
        give(player, reward);
        player.sendSystemMessage(Component.translatable("quasium.progression.reward_bought", reward.getDefaultInstance().getHoverName(), price));
        return true;
    }

    public static Item rewardOf(ProgressionArticle article) {
        ResourceLocation id = article.getReward() == null ? null : ResourceLocation.tryParse(article.getReward());
        return id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
    }

    private static void give(ServerPlayer player, Item reward) {
        ItemStack stack = new ItemStack(reward);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static boolean unlocked(ServerPlayer player, ProgressionArticle article) {
        ResourceLocation id = article.getUnlockCondition() == null ? null : ResourceLocation.tryParse(article.getUnlockCondition());
        AdvancementHolder holder = id == null ? null : player.server.getAdvancements().get(id);
        return holder == null || player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
