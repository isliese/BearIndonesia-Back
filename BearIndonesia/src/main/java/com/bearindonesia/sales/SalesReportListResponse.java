package com.bearindonesia.sales;

import java.time.OffsetDateTime;

public record SalesReportListResponse(
        Long id,
        String title,
        String originalFilename,
        SalesReportCreatedBy createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

