package com.bearindonesia.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ReportService {

    private final JdbcTemplate jdbcTemplate;
    private static final int CLUSTER_LIMIT = 50;
    private static final double CLUSTER_SIM_THRESHOLD = 0.25;
    private static final int PIN_MIN_COUNT = 3;
    private static final double PIN_MULTIPLIER = 2.0;
    private static final int PIN_LOOKBACK_DAYS = 7;
    private static final Map<String, Double> SOURCE_WEIGHTS = buildSourceWeights();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int KEYWORD_RANK_LIMIT = 20;
    private static final int AUTO_COMPETITOR_LIMIT = 6;
    private static final int WEEKLY_ISSUE_DAYS = 7;
    private static final int MONTHLY_ISSUE_DAYS = 30;
    private static final int TREND_KEYWORD_LIMIT = 6;
    private static final int ISSUE_TITLE_LIMIT = 8;
    private static final int ISSUE_TITLE_FETCH_MULTIPLIER = 3;
    private static final Set<String> KEYWORD_STOPWORDS = buildKeywordStopwords();
    private static final List<String> KEYWORD_ALLOWLIST = buildKeywordAllowlist();

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CompetitorReportResponse buildCompetitorReport(LocalDate start, LocalDate end, List<String> keywords, int topLimit) {
        CompetitorReportResponse resp = new CompetitorReportResponse();
        DateRange range = resolveDateRange(start, end);
        resp.start = range.start;
        resp.end = range.end;
        resp.keywords = keywords;
        resp.totals = new ArrayList<>();
        resp.daily = new ArrayList<>();
        resp.sources = new ArrayList<>();
        resp.topArticles = new ArrayList<>();
        resp.clusters = new ArrayList<>();
        resp.insights = new ArrayList<>();
        resp.pins = new ArrayList<>();
        resp.impacts = new ArrayList<>();
        resp.mentionedKeywords = new ArrayList<>();
        resp.keywordRanks = new ArrayList<>();
        resp.autoCompetitors = new ArrayList<>();
        resp.weeklyIssues = new ArrayList<>();
        resp.monthlyIssues = new ArrayList<>();
        resp.weeklyIssueTitles = new ArrayList<>();
        resp.monthlyIssueTitles = new ArrayList<>();
        resp.rangeIssueTitles = new ArrayList<>();
        resp.keywordTrends = new ArrayList<>();

        resp.keywordRanks = buildKeywordRanks(range.start, range.end);
        resp.autoCompetitors = resp.keywordRanks.stream()
            .map(r -> r.keyword)
            .limit(AUTO_COMPETITOR_LIMIT)
            .collect(Collectors.toList());
        resp.weeklyIssues = buildKeywordRanks(range.end.minusDays(WEEKLY_ISSUE_DAYS - 1), range.end);
        resp.monthlyIssues = buildKeywordRanks(range.end.minusDays(MONTHLY_ISSUE_DAYS - 1), range.end);
        resp.weeklyIssueTitles = buildIssueTitles(range.end.minusDays(WEEKLY_ISSUE_DAYS - 1), range.end, ISSUE_TITLE_LIMIT);
        resp.monthlyIssueTitles = buildIssueTitles(range.end.minusDays(MONTHLY_ISSUE_DAYS - 1), range.end, ISSUE_TITLE_LIMIT);
        resp.rangeIssueTitles = buildIssueTitles(range.start, range.end, ISSUE_TITLE_LIMIT);
        resp.keywordTrends = buildKeywordTrends(range.start, range.end);

        if (keywords == null || keywords.isEmpty()) {
            return resp;
        }

        long days = ChronoUnit.DAYS.between(range.start, range.end) + 1;
        LocalDate prevEnd = range.start.minusDays(1);
        LocalDate prevStart = range.start.minusDays(days);

        String baseWhere = "p.is_pharma_related IS TRUE AND r.published_date BETWEEN ? AND ? AND (" +
            "LOWER(COALESCE(p.kor_title, r.title)) LIKE LOWER(CONCAT('%', ?, '%')) OR " +
            "LOWER(COALESCE(p.kor_summary, p.kor_content, r.content)) LIKE LOWER(CONCAT('%', ?, '%'))" +
            ")";

        for (String keyword : keywords) {
            String kw = keyword == null ? "" : keyword.trim();
            if (kw.isEmpty()) {
                continue;
            }

            Integer total = countKeyword(baseWhere, range.start, range.end, kw);
            Integer prevTotal = countKeyword(baseWhere, prevStart, prevEnd, kw);
            CompetitorTotalRow totalRow = new CompetitorTotalRow();
            totalRow.keyword = kw;
            totalRow.count = total == null ? 0 : total;
            totalRow.previousCount = prevTotal == null ? 0 : prevTotal;
            totalRow.delta = totalRow.count - totalRow.previousCount;
            if (totalRow.previousCount > 0) {
                totalRow.changeRate = (double) totalRow.delta / totalRow.previousCount;
            } else {
                totalRow.changeRate = null;
            }
            resp.totals.add(totalRow);

            List<CompetitorDailyRow> dailyRows = jdbcTemplate.query(
                "SELECT r.published_date, COUNT(*) AS cnt " +
                    "FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id " +
                    "WHERE " + baseWhere + " " +
                    "GROUP BY r.published_date ORDER BY r.published_date",
                (rs, rowNum) -> {
                    CompetitorDailyRow row = new CompetitorDailyRow();
                    row.keyword = kw;
                    row.date = rs.getDate("published_date").toLocalDate();
                    row.count = rs.getInt("cnt");
                    return row;
                },
                Date.valueOf(range.start),
                Date.valueOf(range.end),
                kw,
                kw
            );
            resp.daily.addAll(dailyRows);

            List<CompetitorSourceRow> sourceRows = jdbcTemplate.query(
                "SELECT COALESCE(r.source, 'Unknown') AS source, COUNT(*) AS cnt " +
                    "FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id " +
                    "WHERE " + baseWhere + " " +
                    "GROUP BY COALESCE(r.source, 'Unknown') ORDER BY cnt DESC",
                (rs, rowNum) -> {
                    CompetitorSourceRow row = new CompetitorSourceRow();
                    row.keyword = kw;
                    row.source = rs.getString("source");
                    row.count = rs.getInt("cnt");
                    return row;
                },
                Date.valueOf(range.start),
                Date.valueOf(range.end),
                kw,
                kw
            );
            resp.sources.addAll(sourceRows);

            List<CompetitorArticleRow> articleRows = jdbcTemplate.query(
                "SELECT p.id, r.title, r.link, r.published_date, r.source, r.img, p.kor_title, p.kor_summary, p.id_summary, p.importance " +
                    "FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id " +
                    "WHERE " + baseWhere + " " +
                    "ORDER BY p.importance DESC NULLS LAST, r.published_date DESC NULLS LAST, p.id DESC " +
                    "LIMIT ?",
                (rs, rowNum) -> {
                    CompetitorArticleRow row = new CompetitorArticleRow();
                    row.keyword = kw;
                    row.articleId = rs.getLong("id");
                    row.title = rs.getString("title");
                    row.korTitle = rs.getString("kor_title");
                    row.korSummary = rs.getString("kor_summary");
                    row.idSummary = rs.getString("id_summary");
                    row.link = rs.getString("link");
                    row.source = rs.getString("source");
                    row.img = rs.getString("img");
                    Date d = rs.getDate("published_date");
                    row.date = d != null ? d.toLocalDate() : null;
                    Object impObj = rs.getObject("importance");
                    row.importance = impObj == null ? null : ((Number) impObj).intValue();
                    return row;
                },
                Date.valueOf(range.start),
                Date.valueOf(range.end),
                kw,
                kw,
                topLimit
            );
            resp.topArticles.addAll(articleRows);

            List<ClusterArticle> clusterArticles = jdbcTemplate.query(
                "SELECT p.id, r.title, r.link, r.published_date, r.source, r.img, p.kor_title, p.kor_summary, p.id_summary, p.importance " +
                    "FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id " +
                    "WHERE " + baseWhere + " " +
                    "ORDER BY p.importance DESC NULLS LAST, r.published_date DESC NULLS LAST, p.id DESC " +
                    "LIMIT ?",
                (rs, rowNum) -> {
                    ClusterArticle row = new ClusterArticle();
                    row.keyword = kw;
                    row.articleId = rs.getLong("id");
                    row.title = rs.getString("title");
                    row.korTitle = rs.getString("kor_title");
                    row.korSummary = rs.getString("kor_summary");
                    row.idSummary = rs.getString("id_summary");
                    row.link = rs.getString("link");
                    row.source = rs.getString("source");
                    row.img = rs.getString("img");
                    Date d = rs.getDate("published_date");
                    row.date = d != null ? d.toLocalDate() : null;
                    Object impObj = rs.getObject("importance");
                    row.importance = impObj == null ? null : ((Number) impObj).intValue();
                    return row;
                },
                Date.valueOf(range.start),
                Date.valueOf(range.end),
                kw,
                kw,
                CLUSTER_LIMIT
            );

            List<Cluster> clusters = clusterBySimilarity(clusterArticles);
            int clusterIndex = 1;
            for (Cluster c : clusters) {
                CompetitorClusterRow row = new CompetitorClusterRow();
                row.keyword = kw;
                row.clusterId = clusterIndex++;
                row.title = c.title;
                row.count = c.articles.size();
                row.topTitles = c.articles.stream()
                    .map(ClusterArticle::displayTitle)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .limit(3)
                    .collect(Collectors.toList());
                row.sampleArticles = c.articles.stream()
                    .sorted(Comparator.comparing(ClusterArticle::importanceSafe).reversed())
                    .limit(5)
                    .map(ClusterArticle::toArticleRef)
                    .collect(Collectors.toList());
                resp.clusters.add(row);
            }

            CompetitorImpactRow impact = new CompetitorImpactRow();
            impact.keyword = kw;
            impact.score = computeImpactScore(clusterArticles);
            impact.articleCount = clusterArticles.size();
            resp.impacts.add(impact);

            List<CompetitorMentionRow> mentionRows = buildMentionedKeywords(range.start, range.end, kw, baseWhere);
            resp.mentionedKeywords.addAll(mentionRows);

            CompetitorInsightRow insight = new CompetitorInsightRow();
            insight.keyword = kw;
            insight.summary = buildStrategicInsightSummary(
                kw,
                totalRow,
                mentionRows,
                clusterArticles,
                range.start,
                range.end
            );
            resp.insights.add(insight);
        }

        resp.pins = buildPins(resp.daily);
        return resp;
    }

    public static class CompetitorReportResponse {
        public LocalDate start;
        public LocalDate end;
        public List<String> keywords;
        public List<CompetitorTotalRow> totals;
        public List<CompetitorDailyRow> daily;
        public List<CompetitorSourceRow> sources;
        public List<CompetitorArticleRow> topArticles;
        public List<CompetitorClusterRow> clusters;
        public List<CompetitorInsightRow> insights;
        public List<CompetitorPinRow> pins;
        public List<CompetitorImpactRow> impacts;
        public List<CompetitorMentionRow> mentionedKeywords;
        public List<KeywordRankRow> keywordRanks;
        public List<String> autoCompetitors;
        public List<KeywordRankRow> weeklyIssues;
        public List<KeywordRankRow> monthlyIssues;
        public List<IssueTitleRow> weeklyIssueTitles;
        public List<IssueTitleRow> monthlyIssueTitles;
        public List<IssueTitleRow> rangeIssueTitles;
        public List<KeywordTrendRow> keywordTrends;
    }

    public static class CompetitorTotalRow {
        public String keyword;
        public int count;
        public int previousCount;
        public int delta;
        public Double changeRate;
    }

    public static class CompetitorDailyRow {
        public String keyword;
        public LocalDate date;
        public int count;
    }

    public static class CompetitorSourceRow {
        public String keyword;
        public String source;
        public int count;
    }

    public static class CompetitorArticleRow {
        public String keyword;
        public Long articleId;
        public String title;
        public String korTitle;
        public String korSummary;
        public String idSummary;
        public String link;
        public String source;
        public String img;
        public LocalDate date;
        public Integer importance;
    }

    public static class CompetitorClusterRow {
        public String keyword;
        public int clusterId;
        public String title;
        public int count;
        public List<String> topTitles;
        public List<CompetitorArticleRef> sampleArticles;
    }

    public static class CompetitorArticleRef {
        public Long articleId;
        public String title;
        public String link;
        public String source;
        public LocalDate date;
        public Integer importance;
    }

    public static class CompetitorInsightRow {
        public String keyword;
        public String summary;
    }

    public static class CompetitorPinRow {
        public String keyword;
        public LocalDate date;
        public int count;
        public double baseline;
        public double ratio;
    }

    public static class CompetitorImpactRow {
        public String keyword;
        public double score;
        public int articleCount;
    }

    public static class CompetitorMentionRow {
        public String keyword;
        public String tag;
        public int count;
    }

    public static class KeywordRankRow {
        public String keyword;
        public int count;
    }

    public static class KeywordTrendRow {
        public String keyword;
        public List<TrendPoint> points;
    }

    public static class IssueTitleRow {
        public String title;
        public String link;
        public String source;
        public LocalDate date;
    }

    public static class TrendPoint {
        public LocalDate date;
        public int count;
    }

    private static class DateRange {
        LocalDate start;
        LocalDate end;
    }

    private Integer countKeyword(String baseWhere, LocalDate start, LocalDate end, String kw) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id WHERE " + baseWhere,
            Integer.class,
            Date.valueOf(start),
            Date.valueOf(end),
            kw,
            kw
        );
    }

    private DateRange resolveDateRange(LocalDate start, LocalDate end) {
        DateRange range = new DateRange();
        if (start != null && end != null) {
            range.start = start;
            range.end = end;
            return range;
        }
        List<LocalDate> row = jdbcTemplate.query(
            "SELECT MIN(published_date) AS min_date, MAX(published_date) AS max_date FROM raw_news",
            (rs, rn) -> {
                Date min = rs.getDate("min_date");
                Date max = rs.getDate("max_date");
                List<LocalDate> out = new ArrayList<>();
                out.add(min != null ? min.toLocalDate() : LocalDate.now().minusDays(30));
                out.add(max != null ? max.toLocalDate() : LocalDate.now());
                return out;
            }
        ).stream().findFirst().orElse(List.of(LocalDate.now().minusDays(30), LocalDate.now()));
        range.start = start != null ? start : row.get(0);
        range.end = end != null ? end : row.get(1);
        return range;
    }

    private List<KeywordRankRow> buildKeywordRanks(LocalDate start, LocalDate end) {
        List<Object> tagRows = jdbcTemplate.query(
            "SELECT p.tags FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id " +
                "WHERE p.is_pharma_related IS TRUE AND r.published_date BETWEEN ? AND ? AND p.tags IS NOT NULL",
            (rs, rowNum) -> rs.getObject("tags"),
            Date.valueOf(start),
            Date.valueOf(end)
        );

        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> display = new HashMap<>();
        for (Object raw : tagRows) {
            List<String> tags = parseTags(raw);
            Set<String> unique = new HashSet<>(tags);
            for (String tag : unique) {
                if (!isMeaningfulKeyword(tag)) continue;
                String normalized = normalizeKeyword(tag);
                counts.put(normalized, counts.getOrDefault(normalized, 0) + 1);
                display.putIfAbsent(normalized, tag.trim());
            }
        }

        return counts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(KEYWORD_RANK_LIMIT)
            .map(e -> {
                KeywordRankRow row = new KeywordRankRow();
                row.keyword = display.getOrDefault(e.getKey(), e.getKey());
                row.count = e.getValue();
                return row;
            })
            .collect(Collectors.toList());
    }

    private List<IssueTitleRow> buildIssueTitles(LocalDate start, LocalDate end, int limit) {
        int fetchLimit = Math.max(limit * ISSUE_TITLE_FETCH_MULTIPLIER, limit);
        List<IssueTitleRow> rows = jdbcTemplate.query(
            "SELECT p.id, r.title, r.link, r.published_date, r.source, p.kor_title, p.kor_summary, p.id_summary, p.importance " +
                "FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id " +
                "WHERE p.is_pharma_related IS TRUE AND r.published_date BETWEEN ? AND ? " +
                "ORDER BY p.importance DESC NULLS LAST, r.published_date DESC NULLS LAST, p.id DESC " +
                "LIMIT ?",
            (rs, rowNum) -> {
                IssueTitleRow row = new IssueTitleRow();
                String korTitle = rs.getString("kor_title");
                String rawTitle = rs.getString("title");
                String summary = rs.getString("kor_summary");
                if (summary == null || summary.isBlank()) {
                    summary = rs.getString("id_summary");
                }
                row.title = buildIssueLine(summary, korTitle != null && !korTitle.isBlank() ? korTitle : rawTitle);
                row.link = rs.getString("link");
                row.source = rs.getString("source");
                Date d = rs.getDate("published_date");
                row.date = d != null ? d.toLocalDate() : null;
                return row;
            },
            Date.valueOf(start),
            Date.valueOf(end),
            fetchLimit
        );

        List<IssueTitleRow> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (IssueTitleRow row : rows) {
            String key = normalizeTitle(row.title);
            if (key.isBlank() || seen.contains(key)) continue;
            seen.add(key);
            out.add(row);
            if (out.size() >= limit) break;
        }
        return out;
    }

    private List<CompetitorMentionRow> buildMentionedKeywords(LocalDate start, LocalDate end, String keyword, String baseWhere) {
        List<Object> tagRows = jdbcTemplate.query(
            "SELECT p.tags FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id " +
                "WHERE " + baseWhere + " AND p.tags IS NOT NULL",
            (rs, rowNum) -> rs.getObject("tags"),
            Date.valueOf(start),
            Date.valueOf(end),
            keyword,
            keyword
        );

        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> display = new HashMap<>();
        String normalizedKeyword = normalizeKeyword(keyword);
        for (Object raw : tagRows) {
            List<String> tags = parseTags(raw);
            Set<String> unique = new HashSet<>(tags);
            for (String tag : unique) {
                if (!isMeaningfulKeyword(tag)) continue;
                String normalized = normalizeKeyword(tag);
                if (normalized.equals(normalizedKeyword)) continue;
                counts.put(normalized, counts.getOrDefault(normalized, 0) + 1);
                display.putIfAbsent(normalized, tag.trim());
            }
        }

        return counts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(10)
            .map(e -> {
                CompetitorMentionRow row = new CompetitorMentionRow();
                row.keyword = keyword;
                row.tag = display.getOrDefault(e.getKey(), e.getKey());
                row.count = e.getValue();
                return row;
            })
            .collect(Collectors.toList());
    }

    private static String buildIssueLine(String summary, String fallbackTitle) {
        String base = summary != null && !summary.isBlank() ? summary : fallbackTitle;
        if (base == null) return "";
        String trimmed = base.replace("*", "").trim();
        if (trimmed.isEmpty()) return "";
        String[] parts = trimmed.split("[\\.\\!\\?\\u3002\\uFF0E\\uFF01\\uFF1F]|\\n");
        String line = parts.length > 0 ? parts[0].trim() : trimmed;
        if (line.length() > 120) {
            line = line.substring(0, 120).trim();
        }
        return line;
    }

    private List<KeywordTrendRow> buildKeywordTrends(LocalDate start, LocalDate end) {
        List<KeywordRankRow> ranks = buildKeywordRanks(start, end);
        List<String> topKeywords = ranks.stream()
            .map(r -> r.keyword)
            .limit(TREND_KEYWORD_LIMIT)
            .collect(Collectors.toList());
        if (topKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<TagDateRow> rows = jdbcTemplate.query(
            "SELECT r.published_date, p.tags FROM processed_news p JOIN raw_news r ON r.id = p.raw_news_id " +
                "WHERE p.is_pharma_related IS TRUE AND r.published_date BETWEEN ? AND ? AND p.tags IS NOT NULL",
            (rs, rn) -> {
                TagDateRow row = new TagDateRow();
                Date d = rs.getDate("published_date");
                row.date = d != null ? d.toLocalDate() : null;
                row.tags = rs.getObject("tags");
                return row;
            },
            Date.valueOf(start),
            Date.valueOf(end)
        );

        Map<String, Map<LocalDate, Integer>> counts = new HashMap<>();
        for (String kw : topKeywords) {
            counts.put(kw, new HashMap<>());
        }

        for (TagDateRow row : rows) {
            if (row.date == null) continue;
            List<String> tags = parseTags(row.tags);
            Set<String> unique = new HashSet<>(tags);
            for (String kw : topKeywords) {
                if (unique.contains(kw)) {
                    Map<LocalDate, Integer> byDate = counts.get(kw);
                    byDate.put(row.date, byDate.getOrDefault(row.date, 0) + 1);
                }
            }
        }

        List<KeywordTrendRow> out = new ArrayList<>();
        for (String kw : topKeywords) {
            KeywordTrendRow row = new KeywordTrendRow();
            row.keyword = kw;
            row.points = new ArrayList<>();
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                TrendPoint p = new TrendPoint();
                p.date = cursor;
                p.count = counts.getOrDefault(kw, Collections.emptyMap()).getOrDefault(cursor, 0);
                row.points.add(p);
                cursor = cursor.plusDays(1);
            }
            out.add(row);
        }
        return out;
    }

    private static class TagDateRow {
        LocalDate date;
        Object tags;
    }

    private List<String> parseTags(Object raw) {
        if (raw == null) return Collections.emptyList();
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s && !s.isBlank()) {
                    out.add(s.trim());
                } else if (item instanceof Map<?, ?> map) {
                    Object name = map.get("name");
                    if (name instanceof String s && !s.isBlank()) {
                        out.add(s.trim());
                    }
                }
            }
            return out;
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) return Collections.emptyList();
        try {
            JsonNode node = OBJECT_MAPPER.readTree(s);
            List<String> out = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode n : node) {
                    if (n.isTextual()) {
                        out.add(n.asText().trim());
                    } else if (n.isObject() && n.has("name")) {
                        out.add(n.get("name").asText().trim());
                    }
                }
            } else if (node.isObject() && node.has("name")) {
                out.add(node.get("name").asText().trim());
            }
            return out;
        } catch (Exception ignored) {
            // Fall back to comma-separated values
        }
        String[] parts = s.split(",");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static Map<String, Double> buildSourceWeights() {
        Map<String, Double> weights = new HashMap<>();
        for (String s : List.of(
            "Kompas", "Tempo", "CNBC Indonesia", "Detik", "CNN Indonesia",
            "Kontan", "Bisnis", "IDX", "SindoNews", "Viva", "BPOM", "MOH"
        )) {
            weights.put(s.toLowerCase(Locale.ROOT), 1.2);
        }
        return weights;
    }

    private static double computeImpactScore(List<ClusterArticle> articles) {
        double total = 0.0;
        for (ClusterArticle a : articles) {
            double sourceWeight = 1.0;
            if (a.source != null) {
                sourceWeight = SOURCE_WEIGHTS.getOrDefault(a.source.toLowerCase(Locale.ROOT), 1.0);
            }
            double importanceWeight = 0.0;
            if (a.importance != null) {
                importanceWeight = Math.min(1.0, a.importance / 100.0);
            }
            double base = 1.0 + importanceWeight;
            total += base * sourceWeight;
        }
        return Math.round(total * 10.0) / 10.0;
    }

    private static String buildInsightSummary(List<Cluster> clusters) {
        if (clusters == null || clusters.isEmpty()) {
            return "";
        }
        List<String> titles = clusters.stream()
            .sorted(Comparator.comparingInt((Cluster c) -> c.articles.size()).reversed())
            .map(c -> c.title)
            .filter(s -> s != null && !s.isBlank())
            .limit(3)
            .collect(Collectors.toList());
        if (titles.isEmpty()) {
            return "";
        }
        return "Trend summary: " + String.join(" - ", titles);
    }

        private static String buildStrategicInsightSummary(
        String keyword,
        CompetitorTotalRow totalRow,
        List<CompetitorMentionRow> mentionRows,
        List<ClusterArticle> articles,
        LocalDate start,
        LocalDate end
    ) {
        int totalCount = totalRow == null ? 0 : Math.max(0, totalRow.count);
        long periodDays = ChronoUnit.DAYS.between(start, end) + 1;
        if (totalCount == 0) {
            return keyword + "는 최근 " + periodDays + "일 기준으로 집계된 관련 기사가 없어 전략 동향을 판단하기에 데이터가 충분하지 않습니다.";
        }

        String merged = buildInsightSourceText(mentionRows, articles);
        StrategicCategory category = detectStrategicCategory(merged);
        List<String> topKeywords = extractTopMentionKeywords(mentionRows);
        String k1 = topKeywords.size() > 0 ? topKeywords.get(0) : "핵심 키워드";
        String k2 = topKeywords.size() > 1 ? topKeywords.get(1) : "시장 이슈";

        StringBuilder sb = new StringBuilder();
        sb.append("최근 ").append(keyword).append("는 ")
            .append(category.label).append(" 관련 대응이 중심에 있는 흐름으로 보입니다. ");
        sb.append(k1).append(", ").append(k2)
            .append(" 언급이 반복적으로 확인되며 ")
            .append(category.strategyEstimate).append(" 가능성이 있습니다. ");
        sb.append("이는 단기 이슈 대응을 넘어 ")
            .append(category.midLongDirection).append(" 관점에서 해석될 수 있습니다. ");
        sb.append("우리 회사 입장에서는 ")
            .append(category.ourImpact).append(" 가능성을 염두에 두고, ")
            .append(category.competitionImpact).append(" 측면의 모니터링이 필요합니다.");

        if (totalRow != null) {
            if (totalRow.delta > 0) {
                sb.append(" 관련 보도는 최근 증가하는 추세입니다.");
            } else if (totalRow.delta < 0) {
                sb.append(" 관련 보도는 최근 완화되는 흐름입니다.");
            }
        }
        return sb.toString().trim();
    }
    private static String buildInsightSourceText(List<CompetitorMentionRow> mentionRows, List<ClusterArticle> articles) {
        List<String> out = new ArrayList<>();
        if (mentionRows != null) {
            for (CompetitorMentionRow m : mentionRows) {
                if (m != null && m.tag != null && !m.tag.isBlank()) out.add(m.tag);
            }
        }
        if (articles != null) {
            for (ClusterArticle a : articles) {
                if (a == null) continue;
                if (a.korSummary != null && !a.korSummary.isBlank()) out.add(a.korSummary);
                else if (a.idSummary != null && !a.idSummary.isBlank()) out.add(a.idSummary);
                else if (a.korTitle != null && !a.korTitle.isBlank()) out.add(a.korTitle);
                else if (a.title != null && !a.title.isBlank()) out.add(a.title);
            }
        }
        return String.join(" ", out).toLowerCase(Locale.ROOT);
    }

    private static List<String> extractTopMentionKeywords(List<CompetitorMentionRow> mentionRows) {
        if (mentionRows == null || mentionRows.isEmpty()) return Collections.emptyList();
        return mentionRows.stream()
            .filter(m -> m != null && m.tag != null && !m.tag.isBlank())
            .sorted((a, b) -> Integer.compare(b.count, a.count))
            .map(m -> m.tag)
            .distinct()
            .limit(2)
            .collect(Collectors.toList());
    }

        private static StrategicCategory detectStrategicCategory(String text) {
        String t = text == null ? "" : text;
        int investment = countMatches(t, "투자", "주가", "매수", "외국인", "자사주", "수급", "증권사", "목표가");
        int ma = countMatches(t, "합병", "인수", "m&a", "acquisition", "merger", "포트폴리오", "외형 성장");
        int product = countMatches(t, "의약품", "신약", "제품", "임상", "pipeline", "launch", "drug", "medicine");
        int regulation = countMatches(t, "규제", "정책", "정부", "허가", "승인", "bpom", "moh", "policy", "approval");

        int max = Math.max(Math.max(investment, ma), Math.max(product, regulation));
        if (max == 0) return StrategicCategory.DEFAULT;
        if (max == investment) return StrategicCategory.INVESTMENT;
        if (max == ma) return StrategicCategory.MA;
        if (max == product) return StrategicCategory.PRODUCT;
        return StrategicCategory.REGULATION;
    }

    private static int countMatches(String text, String... needles) {
        int score = 0;
        for (String n : needles) {
            if (text.contains(n.toLowerCase(Locale.ROOT))) score++;
        }
        return score;
    }

    private static class StrategicCategory {
        static final StrategicCategory INVESTMENT = new StrategicCategory(
            "투자/주가",
            "시장 내 투자 관심도 확대",
            "자본시장 대응 역량 강화",
            "투자심리 변화",
            "시장 포지셔닝 경쟁"
        );
        static final StrategicCategory MA = new StrategicCategory(
            "M&A/합병",
            "사업 확장 전략 강화",
            "포트폴리오 재편 가능성",
            "시장 재편",
            "경쟁 구도 변화"
        );
        static final StrategicCategory PRODUCT = new StrategicCategory(
            "제품/신약/임상",
            "제품 포트폴리오 확장",
            "연구개발 기반 성장 모델 강화",
            "동일 치료영역 경쟁 심화",
            "시장 점유율 변동"
        );
        static final StrategicCategory REGULATION = new StrategicCategory(
            "규제/정책",
            "규제 환경 변화 대응",
            "승인 및 제도 대응 체계 고도화",
            "규제 대응 부담",
            "승인 속도 경쟁"
        );
        static final StrategicCategory DEFAULT = new StrategicCategory(
            "사업/시장",
            "사업 전략 재정비",
            "중장기 전략 방향 조정",
            "경쟁 환경 변화",
            "시장 대응 전략"
        );

        final String label;
        final String strategyEstimate;
        final String midLongDirection;
        final String ourImpact;
        final String competitionImpact;

        StrategicCategory(String label, String strategyEstimate, String midLongDirection, String ourImpact, String competitionImpact) {
            this.label = label;
            this.strategyEstimate = strategyEstimate;
            this.midLongDirection = midLongDirection;
            this.ourImpact = ourImpact;
            this.competitionImpact = competitionImpact;
        }
    }
    private static List<CompetitorPinRow> buildPins(List<CompetitorDailyRow> dailyRows) {
        if (dailyRows == null || dailyRows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<CompetitorDailyRow>> byKeyword = new HashMap<>();
        for (CompetitorDailyRow row : dailyRows) {
            byKeyword.computeIfAbsent(row.keyword, k -> new ArrayList<>()).add(row);
        }
        List<CompetitorPinRow> pins = new ArrayList<>();
        for (Map.Entry<String, List<CompetitorDailyRow>> entry : byKeyword.entrySet()) {
            List<CompetitorDailyRow> rows = entry.getValue();
            rows.sort(Comparator.comparing(r -> r.date));
            ArrayDeque<Integer> window = new ArrayDeque<>();
            double sum = 0.0;
            for (CompetitorDailyRow row : rows) {
                double baseline = window.isEmpty() ? 0.0 : sum / window.size();
                if (baseline > 0 && row.count >= Math.max(PIN_MIN_COUNT, baseline * PIN_MULTIPLIER)) {
                    CompetitorPinRow pin = new CompetitorPinRow();
                    pin.keyword = row.keyword;
                    pin.date = row.date;
                    pin.count = row.count;
                    pin.baseline = Math.round(baseline * 10.0) / 10.0;
                    pin.ratio = Math.round((row.count / baseline) * 10.0) / 10.0;
                    pins.add(pin);
                }
                window.addLast(row.count);
                sum += row.count;
                if (window.size() > PIN_LOOKBACK_DAYS) {
                    sum -= window.removeFirst();
                }
            }
        }
        return pins;
    }

    private static List<Cluster> clusterBySimilarity(List<ClusterArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Cluster> clusters = new ArrayList<>();
        for (ClusterArticle article : articles) {
            String text = article.titleForSimilarity();
            Set<String> tokens = tokenize(text);
            Cluster bestCluster = null;
            double bestScore = 0.0;
            for (Cluster cluster : clusters) {
                double sim = jaccard(tokens, cluster.tokens);
                if (sim > bestScore) {
                    bestScore = sim;
                    bestCluster = cluster;
                }
            }
            if (bestCluster != null && bestScore >= CLUSTER_SIM_THRESHOLD) {
                bestCluster.articles.add(article);
                bestCluster.tokens = union(bestCluster.tokens, tokens);
            } else {
                Cluster cluster = new Cluster();
                cluster.title = article.displayTitle();
                cluster.tokens = tokens;
                cluster.articles = new ArrayList<>();
                cluster.articles.add(article);
                clusters.add(cluster);
            }
        }
        clusters.sort(Comparator.comparingInt((Cluster c) -> c.articles.size()).reversed());
        return clusters;
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> out = new HashSet<>(a);
        out.addAll(b);
        return out;
    }

    private static Set<String> tokenize(String text) {
        if (text == null) {
            return Collections.emptySet();
        }
        String normalized = text.toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
            .trim();
        if (normalized.isEmpty()) {
            return Collections.emptySet();
        }
        String[] parts = normalized.split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String p : parts) {
            if (p.length() < 2) {
                continue;
            }
            tokens.add(p);
        }
        return tokens;
    }

    private static boolean isMeaningfulKeyword(String keyword) {
        if (keyword == null) return false;
        String normalized = normalizeKeyword(keyword);
        if (normalized.isBlank()) return false;
        if (isAllowlistedKeyword(normalized)) return true;
        if (normalized.length() < 3) return false;
        if (normalized.matches("\\d+")) return false;
        if (KEYWORD_STOPWORDS.contains(normalized)) return false;
        return true;
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) return "";
        String normalized = keyword.toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
            .trim();
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }

    private static String normalizeTitle(String title) {
        if (title == null) return "";
        String normalized = title.toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
            .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private static boolean isAllowlistedKeyword(String normalized) {
        for (String token : KEYWORD_ALLOWLIST) {
            if (normalized.contains(token)) return true;
        }
        return false;
    }

    private static Set<String> buildKeywordStopwords() {
        Set<String> out = new HashSet<>();
        Collections.addAll(out,
            "industri", "farmasi", "industri farmasi", "perusahaan", "pasar", "produk",
            "pemerintah", "indonesia", "nasional", "tahun", "harga", "kesehatan",
            "obat", "obatan", "pharma", "pharmaceutical", "industry", "market",
            "company", "companies", "produk obat", "obat generik", "obat baru",
            "pertumbuhan", "penjualan", "laba", "rugi", "saham", "investasi",
            "regulasi", "kebijakan", "kementerian", "otoritas", "program", "peluang",
            "riset", "penelitian", "kinerja", "target", "realisasi", "permintaan",
            "bpom", "kemenkes", "kementerian kesehatan", "kementerian kesehatan ri",
            "kementerian kesehatan republik indonesia", "bpjs", "bpjs kesehatan", "jkn",
            "asuransi", "asuransi kesehatan", "jaminan kesehatan", "izin edar",
            "gmp", "kosmetik", "cosmetic", "cosmetics", "traditional medicine",
            "kerjasama", "kolaborasi", "kerja sama", "kerja-sama",
            "ekspor", "impor", "bisnis", "investasi farmasi"
        );
        return out;
    }

    private static List<String> buildKeywordAllowlist() {
        List<String> out = new ArrayList<>();
        Collections.addAll(out,
            "izin edar", "izin", "registrasi", "registration", "approval", "approved",
            "authorization", "clearance", "tracking", "follow up",
            "new drug", "new medicine", "novel drug", "vaksin", "vaccine",
            "biosimilar", "biologic", "api", "bahan baku",
            "clinical", "clinical trial", "phase i", "phase ii", "phase iii",
            "produk", "product", "brand", "trade name"
        );
        return out;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int inter = 0;
        for (String token : a) {
            if (b.contains(token)) inter++;
        }
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0.0 : (double) inter / union;
    }

    private static class Cluster {
        String title;
        Set<String> tokens;
        List<ClusterArticle> articles;
    }

    private static class ClusterArticle {
        String keyword;
        Long articleId;
        String title;
        String korTitle;
        String korSummary;
        String idSummary;
        String link;
        String source;
        String img;
        LocalDate date;
        Integer importance;

        String displayTitle() {
            if (korTitle != null && !korTitle.isBlank()) {
                return korTitle;
            }
            return title;
        }

        String titleForSimilarity() {
            String base = displayTitle();
            String summary = korSummary != null && !korSummary.isBlank() ? korSummary : idSummary;
            if (summary != null && !summary.isBlank()) {
                return base + " " + summary;
            }
            return base;
        }

        int importanceSafe() {
            return importance == null ? 0 : importance;
        }

        CompetitorArticleRef toArticleRef() {
            CompetitorArticleRef ref = new CompetitorArticleRef();
            ref.articleId = articleId;
            ref.title = displayTitle();
            ref.link = link;
            ref.source = source;
            ref.date = date;
            ref.importance = importance;
            return ref;
        }
    }
}





