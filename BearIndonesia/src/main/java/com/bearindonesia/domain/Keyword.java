package com.bearindonesia.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "news_tags", indexes = {
    @Index(name = "ix_news_tags_news_id", columnList = "news_id")
})
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    @JsonIgnore
    private Article article;

    public Keyword() {}
    public Keyword(String name) { this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
}

