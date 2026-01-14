package com.example.bearindonesia.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.bearindonesia.domain.Article;
import com.example.bearindonesia.repository.ArticleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DataLoader implements CommandLineRunner {

    private final ArticleRepository articleRepository;

    public DataLoader(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            loadJsonToDb("news.json");
            loadJsonToDb("BPOM.json");
            loadJsonToDb("CNN.json");
            loadJsonToDb("Detik.json");
            loadJsonToDb("MOH.json");
            loadJsonToDb("CNBC.json");
        } catch (Exception e) {
            System.err.println("Data load failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadJsonToDb(String fileName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = TypeReference.class.getResourceAsStream("/" + fileName);
            if (inputStream == null) {
                System.err.println(fileName + " not found");
                return;
            }

            JsonNode jsonArray = mapper.readTree(inputStream);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int count = 0;
            for (JsonNode articleNode : jsonArray) {
                Article article = new Article();

                if (articleNode.has("id") && !articleNode.get("id").isNull()) {
                    article.setId(Long.parseLong(articleNode.get("id").asText()));
                }

                article.setTitle(getSafeText(articleNode, "title"));
                article.setKorTitle(getSafeText(articleNode, "korTitle"));
                article.setEngTitle(getSafeText(articleNode, "engTitle"));
                article.setContent(getSafeText(articleNode, "content"));
                article.setLink(getSafeText(articleNode, "link"));

                if (articleNode.has("date") && !articleNode.get("date").isNull()) {
                    String dateStr = articleNode.get("date").asText();
                    if (!dateStr.isBlank()) {
                        article.setDate(LocalDate.parse(dateStr, formatter));
                    }
                }

                article.setCategory(getSafeText(articleNode, "category"));
                article.setEngCategory(getSafeText(articleNode, "engCategory"));
                article.setSource(getSafeText(articleNode, "source"));
                article.setKorSummary(getSafeText(articleNode, "korSummary"));
                article.setEngSummary(getSafeText(articleNode, "engSummary"));
                article.setTranslated(getSafeText(articleNode, "translated"));

                if (articleNode.has("importance") && articleNode.get("importance").isInt()) {
                    article.setImportance(articleNode.get("importance").asInt());
                }

                article.setImportanceRationale(getSafeText(articleNode, "importanceRationale"));

                if (articleNode.has("tags") && articleNode.get("tags").isArray()) {
                    article.setTagsJson(articleNode.get("tags").toString());
                }

                articleRepository.save(article);
                count++;
            }

            System.out.println(fileName + " loaded: " + count + " rows");
        } catch (Exception e) {
            System.err.println(fileName + " load failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getSafeText(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asText() : null;
    }
}
