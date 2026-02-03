package com.bearindonesia.wordcloud;

import java.time.Instant;

public record WordCloudResponse(String imageUrl, Instant expiresAt) {
}
