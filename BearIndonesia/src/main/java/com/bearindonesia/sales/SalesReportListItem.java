package com.bearindonesia.sales;

import java.time.OffsetDateTime;

public record SalesReportListItem(
        Long id,
        String title,
        String originalFilename,
        Long createdById,
        String createdByName,
        String createdByEmail,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
