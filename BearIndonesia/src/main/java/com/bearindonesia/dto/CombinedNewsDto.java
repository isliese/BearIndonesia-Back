package com.bearindonesia.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class CombinedNewsDto {
    // raw_news 테이블 컬럼 매핑
    public Long id;
    public String title;
    public String link;
    public String img;
    public String content;
    public LocalDate publishedDate;
    public String source;
    public String searchKeyword;
    public String language;
    public OffsetDateTime crawledAt;
    public JsonNode rawPayload;

    // processed_news 테이블 컬럼 매핑
    public Long rawNewsId;
    public String korTitle;
    public String korSummary;
    public String korContent;
    public String category;
    public String engCategory;
    public JsonNode tags;
    public Integer importance;
    public String importanceRationale;
    public Boolean isPharmaRelated;
    public String titleFilterReason;
    public String model;
    public String promptVersion;
    public Integer tokensIn;
    public Integer tokensOut;
    public String status;
    public String errorMessage;
    public OffsetDateTime processedAt;
}
