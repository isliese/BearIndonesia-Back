package com.example.bearindonesia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleDto {
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
    public String translated;

    public Integer importance;
    public String importanceRationale;

    public List<KeywordDto> tags;
}
