package com.bearindonesia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsletterCoreNewsItemDto {
    public String title;
    public String img;
}
