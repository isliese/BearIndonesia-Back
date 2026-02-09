package com.bearindonesia.newsletter;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;
    private final JdbcTemplate jdbcTemplate;

    public NewsletterService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${newsletter.python.base-url}") String pythonBaseUrl,
            JdbcTemplate jdbcTemplate
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.pythonBaseUrl = pythonBaseUrl;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResponseEntity<byte[]> fetchNewsletter(int year, int month) {
        ResponseEntity<byte[]> cached = fetchCachedNewsletter(year, month);
        if (cached != null) {
            log.info("Newsletter cache hit ({}-{}).", year, month);
            return cached;
        }
        log.info("Newsletter cache miss ({}-{}). Fetching from Python.", year, month);

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

        cacheNewsletter(year, month, body);
        return response;
    }

    private ResponseEntity<byte[]> fetchCachedNewsletter(int year, int month) {
        try {
            String html = jdbcTemplate.queryForObject(
                    "SELECT html FROM public.newsletter_monthly WHERE year = ? AND month = ?",
                    String.class,
                    year,
                    month
            );
            if (html == null || html.isBlank()) {
                return null;
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html.getBytes(StandardCharsets.UTF_8));
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        } catch (DataAccessException e) {
            log.warn("Failed to read cached newsletter ({}-{}). Falling back to Python.", year, month, e);
            return null;
        }
    }

    private void cacheNewsletter(int year, int month, byte[] body) {
        try {
            String html = new String(body, StandardCharsets.UTF_8);
            int updated = jdbcTemplate.update(
                    "UPDATE public.newsletter_monthly SET html = ?, updated_at = now() WHERE year = ? AND month = ?",
                    html,
                    year,
                    month
            );
            if (updated > 0) {
                log.info("Newsletter cached via UPDATE ({}-{}).", year, month);
                return;
            }

            int inserted = jdbcTemplate.update(
                    "INSERT INTO public.newsletter_monthly (year, month, html) VALUES (?, ?, ?)",
                    year,
                    month,
                    html
            );
            if (inserted > 0) {
                log.info("Newsletter cached via INSERT ({}-{}).", year, month);
            }
        } catch (DataAccessException e) {
            log.warn("Failed to cache newsletter ({}-{}). Returning response without caching.", year, month, e);
        }
    }
}
