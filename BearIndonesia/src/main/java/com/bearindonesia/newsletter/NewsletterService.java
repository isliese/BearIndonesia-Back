package com.bearindonesia.newsletter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NewsletterService {

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;

    public NewsletterService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${newsletter.python.base-url}") String pythonBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.pythonBaseUrl = pythonBaseUrl;
    }

    public ResponseEntity<byte[]> fetchNewsletter(int year, int month) {
        String base = pythonBaseUrl.endsWith("/")
                ? pythonBaseUrl + "newsletter"
                : pythonBaseUrl + "/newsletter";
        String url = UriComponentsBuilder.fromHttpUrl(base)
                .queryParam("year", year)
                .queryParam("month", String.format("%02d", month))
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new IllegalArgumentException("뉴스레터 결과를 받지 못했습니다.");
        }
        return response;
    }
}
