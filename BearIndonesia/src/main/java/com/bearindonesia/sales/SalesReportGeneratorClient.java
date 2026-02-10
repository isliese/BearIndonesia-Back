package com.bearindonesia.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

@Component
public class SalesReportGeneratorClient {

    private static final Logger log = LoggerFactory.getLogger(SalesReportGeneratorClient.class);

    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;
    private final String insightsLanguage;

    public SalesReportGeneratorClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${sales.python.base-url}") String pythonBaseUrl,
            @Value("${sales.report.insights-language:ko}") String insightsLanguage
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.pythonBaseUrl = pythonBaseUrl;
        this.insightsLanguage = insightsLanguage;
    }

    public String generateHtml(String title, MultipartFile file) {
        Objects.requireNonNull(file, "file");
        String originalFilename = file.getOriginalFilename() == null ? "upload.xlsx" : file.getOriginalFilename();

        try {
            String base = pythonBaseUrl.endsWith("/")
                    ? pythonBaseUrl + "sales/report"
                    : pythonBaseUrl + "/sales/report";
            String url = UriComponentsBuilder.fromHttpUrl(base).toUriString();

            byte[] bytes = file.getBytes();
            ByteArrayResource fileResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", fileResource);
            if (title != null && !title.isBlank()) {
                form.add("title", title);
            }
            if (insightsLanguage != null && !insightsLanguage.isBlank()) {
                form.add("insightsLanguage", insightsLanguage);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, headers);

            ResponseEntity<GenerateResponse> resp = restTemplate.postForEntity(url, entity, GenerateResponse.class);
            GenerateResponse body = resp.getBody();
            if (body == null || body.html == null || body.html.isBlank()) {
                throw new IllegalArgumentException("리포트 생성 결과(html)를 받지 못했습니다.");
            }
            return body.html;
        } catch (Exception e) {
            log.warn("Sales report generation via Python failed.", e);
            throw new IllegalArgumentException("리포트 생성에 실패했습니다: " + safeMessage(e), e);
        }
    }

    private static String safeMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return "unknown error";
        }
        msg = msg.trim();
        return msg.length() > 300 ? msg.substring(0, 300) : msg;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerateResponse {
        public String html;
    }
}
