package net.sorukoma.quasium.client;

import net.sorukoma.quasium.progression.ProgressionArticle;

import java.util.HashMap;
import java.util.Map;

/**
 * Клиентское хранилище статей прогрессии. Статьи определяются в датапаке и грузятся
 * только на серверной стороне (ProgressionArticleManager). На выделенном сервере у
 * клиента нет доступа к серверному менеджеру, поэтому сервер присылает статьи пакетом
 * ProgressionArticlesSyncPacket (при входе игрока и при /reload), а книга прогрессии
 * читает их отсюда. Ключ — заголовок статьи (article.getTitle()).
 */
public class ClientProgressionData {
    private static Map<String, ProgressionArticle> articles = new HashMap<>();

    public static void setArticles(Map<String, ProgressionArticle> newArticles) {
        articles = newArticles;
    }

    public static Map<String, ProgressionArticle> getArticles() {
        return articles;
    }
}
