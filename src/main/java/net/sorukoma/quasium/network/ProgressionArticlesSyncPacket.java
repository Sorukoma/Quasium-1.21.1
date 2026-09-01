package net.sorukoma.quasium.network;

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
import net.sorukoma.quasium.progression.ProgressionPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-to-Client: передаёт клиенту определения статей прогрессии из датапака.
 * Нужен потому, что ProgressionArticleManager — серверный reload-listener, и на
 * выделенном сервере у клиента нет этих данных напрямую.
 */
public record ProgressionArticlesSyncPacket(List<ProgressionArticle> articles) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ProgressionArticlesSyncPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "progression_articles_sync"));
    public static final StreamCodec<FriendlyByteBuf, ProgressionArticlesSyncPacket> STREAM_CODEC =
            StreamCodec.of(ProgressionArticlesSyncPacket::encode, ProgressionArticlesSyncPacket::decode);

    private static void writeNullableUtf(FriendlyByteBuf buf, String value) {
        boolean present = value != null;
        buf.writeBoolean(present);
        if (present) {
            buf.writeUtf(value);
        }
    }

    private static String readNullableUtf(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUtf() : null;
    }

    public static void encode(FriendlyByteBuf buf, ProgressionArticlesSyncPacket packet) {
        buf.writeVarInt(packet.articles().size());
        for (ProgressionArticle article : packet.articles()) {
            writeNullableUtf(buf, article.getTitle());
            writeNullableUtf(buf, article.getUnlockCondition());
            writeNullableUtf(buf, article.getHint());

            List<ProgressionPage> pages = article.getPages();
            if (pages == null) {
                buf.writeVarInt(0);
            } else {
                buf.writeVarInt(pages.size());
                for (ProgressionPage page : pages) {
                    writeNullableUtf(buf, page.getText());
                }
            }
        }
    }

    public static ProgressionArticlesSyncPacket decode(FriendlyByteBuf buf) {
        int articleCount = buf.readVarInt();
        List<ProgressionArticle> articles = new ArrayList<>(articleCount);
        for (int i = 0; i < articleCount; i++) {
            ProgressionArticle article = new ProgressionArticle();
            article.setTitle(readNullableUtf(buf));
            article.setUnlockCondition(readNullableUtf(buf));
            article.setHint(readNullableUtf(buf));

            int pageCount = buf.readVarInt();
            List<ProgressionPage> pages = new ArrayList<>(pageCount);
            for (int p = 0; p < pageCount; p++) {
                ProgressionPage page = new ProgressionPage();
                page.setText(readNullableUtf(buf));
                pages.add(page);
            }
            article.setPages(pages);
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
        Map<String, ProgressionArticle> byTitle = new HashMap<>();
        for (ProgressionArticle article : packet.articles()) {
            if (article.getTitle() != null) {
                byTitle.put(article.getTitle(), article);
            }
        }
        ClientProgressionData.setArticles(byTitle);
    }
}
