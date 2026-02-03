package com.bearindonesia.wordcloud;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WordCloudService {

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;

    public WordCloudService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${wordcloud.python.base-url}") String pythonBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.pythonBaseUrl = pythonBaseUrl;
    }

    public byte[] generate(WordCloudRequest request) {
        String url = pythonBaseUrl.endsWith("/")
                ? pythonBaseUrl + "wordcloud"
                : pythonBaseUrl + "/wordcloud";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WordCloudRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new IllegalArgumentException("워드클라우드 결과를 받지 못했습니다.");
        }
        return body;
    }
}
