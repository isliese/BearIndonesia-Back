package com.bearindonesia.api;

import com.bearindonesia.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/competitors")
    public ReportService.CompetitorReportResponse competitors(
        @RequestParam(required = false) String start,
        @RequestParam(required = false) String end,
        @RequestParam(required = false) String keywords,
        @RequestParam(required = false, defaultValue = "10") int topLimit
    ) {
        LocalDate startDate = start == null || start.isBlank() ? null : LocalDate.parse(start);
        LocalDate endDate = end == null || end.isBlank() ? null : LocalDate.parse(end);
        List<String> keywordList = parseKeywords(keywords);
        return reportService.buildCompetitorReport(startDate, endDate, keywordList, topLimit);
    }

    private List<String> parseKeywords(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
}
