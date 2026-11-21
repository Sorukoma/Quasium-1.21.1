package net.sorukoma.quasium.network;

import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientProgressionData;
import net.sorukoma.quasium.progression.ProgressionArticle;

import java.util.ArrayList;
import java.util.List;

public record ProgressionArticlesSyncPacket(List<ProgressionArticle> articles) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ProgressionArticlesSyncPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression_articles_sync"));
    public static final StreamCodec<FriendlyByteBuf, ProgressionArticlesSyncPacket> STREAM_CODEC =
            StreamCodec.of(ProgressionArticlesSyncPacket::encode, ProgressionArticlesSyncPacket::decode);
    private static final Gson GSON = new Gson();

    public static void encode(FriendlyByteBuf buf, ProgressionArticlesSyncPacket packet) {
        buf.writeVarInt(packet.articles().size());
        for (ProgressionArticle article : packet.articles()) {
            buf.writeResourceLocation(article.getId());
            buf.writeUtf(GSON.toJson(article));
        }
    }

    public static ProgressionArticlesSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ProgressionArticle> articles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            ProgressionArticle article = GSON.fromJson(buf.readUtf(), ProgressionArticle.class);
            article.setId(id);
            articles.add(article);
        }
        return new ProgressionArticlesSyncPacket(articles);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ProgressionArticlesSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                handleClient(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ProgressionArticlesSyncPacket packet) {
        ClientProgressionData.setArticles(packet.articles());
    }
}
