package com.bearindonesia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleDto {
    public Long id;
    public Long rawNewsId;
    public String title;
    public String korTitle;
    public String engTitle;
    public String link;
    public String content;
    public LocalDate date;
    public String source;

    public String category;
    public String engCategory;

    public String korSummary;
    public String engSummary;
    public String idSummary;

    public Float semanticConfidence;
    public Boolean tagMismatch;
    public Boolean categoryMismatch;
    public String korContent;

    public Integer importance;
    public String insight;

    public List<KeywordDto> tags;
}
