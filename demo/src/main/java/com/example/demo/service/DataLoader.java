package com.example.demo.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.entity.News;
import com.example.demo.repository.NewsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DataLoader implements CommandLineRunner {

    private final NewsRepository newsRepository;

    public DataLoader(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
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
            System.err.println("데이터 로딩 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadJsonToDb(String fileName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = TypeReference.class.getResourceAsStream("/" + fileName);
            if (inputStream == null) {
                System.err.println(fileName + " 파일을 찾을 수 없습니다!");
                return;
            }

            JsonNode jsonArray = mapper.readTree(inputStream);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int count = 0;
            for (JsonNode newsNode : jsonArray) {
                News news = new News();

                if (newsNode.has("id") && !newsNode.get("id").isNull()) {
                    news.setId(Long.parseLong(newsNode.get("id").asText()));
                }

                news.setTitle(getSafeText(newsNode, "title"));
                news.setKorTitle(getSafeText(newsNode, "korTitle"));
                news.setEngTitle(getSafeText(newsNode, "engTitle"));
                news.setContent(getSafeText(newsNode, "content"));
                news.setLink(getSafeText(newsNode, "link"));

                if (newsNode.has("date") && !newsNode.get("date").isNull()) {
                    String dateStr = newsNode.get("date").asText();
                    if (!dateStr.isBlank()) {
                        news.setDate(LocalDate.parse(dateStr, formatter));
                    }
                }

                news.setCategory(getSafeText(newsNode, "category"));
                news.setEngCategory(getSafeText(newsNode, "engCategory"));
                news.setSource(getSafeText(newsNode, "source"));
                news.setKorSummary(getSafeText(newsNode, "korSummary"));
                news.setEngSummary(getSafeText(newsNode, "engSummary"));
                news.setTranslated(getSafeText(newsNode, "translated"));

                if (newsNode.has("importance") && newsNode.get("importance").isInt()) {
                    news.setImportance(newsNode.get("importance").asInt());
                }

                news.setImportanceRationale(getSafeText(newsNode, "importanceRationale"));

                if (newsNode.has("tags") && newsNode.get("tags").isArray()) {
                    news.setTagsJson(newsNode.get("tags").toString());
                }

                newsRepository.save(news);
                count++;
            }

            System.out.println("✅ " + fileName + " 데이터 " + count + "건이 DB에 저장되었습니다!");
        } catch (Exception e) {
            System.err.println(fileName + " 로딩 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getSafeText(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asText() : null;
    }
}
