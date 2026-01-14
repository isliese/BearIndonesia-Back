package com.example.bearindonesia.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Article {

    @Id
    private Long id;

    @Column(length = 1000)
    private String title;

    @Column(length = 1000)
    private String korTitle;

    @Column(length = 1000)
    private String engTitle;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, unique = true)
    private String link;

    private LocalDate date;

    private String category;
    private String engCategory;
    private String source;

    @Column(columnDefinition = "TEXT")
    private String korSummary;

    @Column(columnDefinition = "TEXT")
    private String engSummary;

    @Column(columnDefinition = "TEXT")
    private String translated;

    private Integer importance;

    @Column(columnDefinition = "TEXT")
    private String importanceRationale;

    @Column(columnDefinition = "TEXT")
    private String tagsJson;
}
