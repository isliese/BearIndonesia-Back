package com.bearindonesia.service;

import com.bearindonesia.dto.ArticleDto;
import com.bearindonesia.dto.KeywordDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;

@Service
public class ArticleService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArticleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ArticleDto> searchProcessedArticles(String query, String sortBy, String filterType) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String normalizedSort = sortBy == null ? "relevance" : sortBy.toLowerCase();
        String normalizedFilter = filterType == null ? "all" : filterType.toLowerCase();
        String sql = """
            SELECT
                p.id,
                p.raw_news_id,
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                r.img,
                p.kor_title,
                p.kor_summary,
                p.id_summary,
                p.semantic_confidence,
                p.tag_mismatch,
                p.category_mismatch,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.insight,
                p.tags
            FROM processed_news p
            JOIN raw_news r ON r.id = p.raw_news_id
            WHERE p.is_pharma_related IS TRUE
              AND (
                  LOWER(COALESCE(p.kor_title, r.title)) LIKE LOWER(CONCAT('%', ?, '%'))
               OR LOWER(COALESCE(p.kor_summary, p.kor_content, r.content)) LIKE LOWER(CONCAT('%', ?, '%'))
              )
              AND (
                  ? = 'all'
               OR LOWER(COALESCE(p.tags::text, '')) LIKE LOWER(CONCAT('%', ?, '%'))
              )
            ORDER BY
                CASE WHEN ? = 'date' THEN r.published_date END DESC NULLS LAST,
                CASE WHEN ? = 'importance' THEN p.importance END DESC NULLS LAST,
                p.importance DESC NULLS LAST,
                r.published_date DESC NULLS LAST,
                p.id DESC
            """;
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> toDto(rs),
            query,
            query,
            normalizedFilter,
            normalizedFilter,
            normalizedSort,
            normalizedSort
        );
    }

    public List<ArticleDto> listProcessedArticles() {
        String sql = """
            SELECT
                p.id,
                p.raw_news_id,
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                r.img,
                p.kor_title,
                p.kor_summary,
                p.id_summary,
                p.semantic_confidence,
                p.tag_mismatch,
                p.category_mismatch,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.insight,
                p.tags
            FROM processed_news p
            JOIN raw_news r ON r.id = p.raw_news_id
            WHERE p.is_pharma_related IS TRUE
            ORDER BY
                p.importance DESC NULLS LAST,
                r.published_date DESC NULLS LAST,
                p.id DESC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> toDto(rs));
    }

    public List<ArticleDto> listProcessedArticlesByMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        String sql = """
            SELECT
                p.id,
                p.raw_news_id,
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                r.img,
                p.kor_title,
                p.kor_summary,
                p.id_summary,
                p.semantic_confidence,
                p.tag_mismatch,
                p.category_mismatch,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.insight,
                p.tags
            FROM processed_news p
            JOIN raw_news r ON r.id = p.raw_news_id
            WHERE p.is_pharma_related IS TRUE
              AND r.published_date BETWEEN ? AND ?
            ORDER BY
                r.published_date DESC NULLS LAST,
                p.id DESC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> toDto(rs), start, end);
    }

    public byte[] exportProcessedArticlesExcel(int year, int month) {
        List<ArticleDto> rows = listProcessedArticlesByMonth(year, month);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet koreanSheet = workbook.createSheet("Korean");
            Sheet indonesianSheet = workbook.createSheet("Indonesian");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

            CellStyle linkStyle = workbook.createCellStyle();
            Font linkFont = workbook.createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            linkStyle.setFont(linkFont);

            CreationHelper creationHelper = workbook.getCreationHelper();

            String[] koreanColumns = new String[] {
                "날짜",
                "출처",
                "카테고리",
                "태그",
                "헤드라인(한글)",
                "요약(한글)",
                "본문(한글)",
                "인사이트",
                "링크",
                "중요도"
            };
            Row koreanHeader = koreanSheet.createRow(0);
            for (int i = 0; i < koreanColumns.length; i++) {
                Cell cell = koreanHeader.createCell(i);
                cell.setCellValue(koreanColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            koreanSheet.setAutoFilter(new CellRangeAddress(0, 0, 0, koreanColumns.length - 1));
            koreanSheet.createFreezePane(0, 1);

            String[] indonesianColumns = new String[] {
                "날짜",
                "출처",
                "헤드라인(인도네시아어)",
                "요약(인도네시아어)",
                "본문(인도네시아어)",
                "링크"
            };
            Row indonesianHeader = indonesianSheet.createRow(0);
            for (int i = 0; i < indonesianColumns.length; i++) {
                Cell cell = indonesianHeader.createCell(i);
                cell.setCellValue(indonesianColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            indonesianSheet.setAutoFilter(new CellRangeAddress(0, 0, 0, indonesianColumns.length - 1));
            indonesianSheet.createFreezePane(0, 1);

            int koreanRowIdx = 1;
            int indonesianRowIdx = 1;
            for (ArticleDto a : rows) {
                Row koreanRow = koreanSheet.createRow(koreanRowIdx++);
                koreanRow.setHeightInPoints(60);
                Cell koreanDateCell = koreanRow.createCell(0);
                if (a.date != null) {
                    koreanDateCell.setCellValue(java.sql.Date.valueOf(a.date));
                    koreanDateCell.setCellStyle(dateStyle);
                } else {
                    koreanDateCell.setCellValue("");
                }
                koreanRow.createCell(1).setCellValue(a.source != null ? a.source : "");
                koreanRow.createCell(2).setCellValue(a.category != null ? a.category : "");
                koreanRow.createCell(3).setCellValue(a.tags != null
                    ? a.tags.stream().map(t -> t.name).reduce("", (acc, name) -> acc.isEmpty() ? name : acc + ", " + name)
                    : "");
                koreanRow.createCell(4).setCellValue(a.korTitle != null ? a.korTitle : "");
                Cell korSummaryCell = koreanRow.createCell(5);
                korSummaryCell.setCellValue(a.korSummary != null ? a.korSummary : "");
                korSummaryCell.setCellStyle(wrapStyle);
                Cell korContentCell = koreanRow.createCell(6);
                korContentCell.setCellValue(a.korContent != null ? a.korContent : "");
                korContentCell.setCellStyle(wrapStyle);
                Cell insightCell = koreanRow.createCell(7);
                insightCell.setCellValue(a.insight != null ? a.insight : "");
                insightCell.setCellStyle(wrapStyle);
                Cell koreanLinkCell = koreanRow.createCell(8);
                if (a.link != null && !a.link.isBlank()) {
                    koreanLinkCell.setCellValue(a.link);
                    koreanLinkCell.setCellStyle(linkStyle);
                    var link = creationHelper.createHyperlink(HyperlinkType.URL);
                    link.setAddress(a.link);
                    koreanLinkCell.setHyperlink(link);
                } else {
                    koreanLinkCell.setCellValue("");
                }
                if (a.importance != null) {
                    koreanRow.createCell(9).setCellValue(a.importance.doubleValue());
                } else {
                    koreanRow.createCell(9).setCellValue("");
                }

                Row indonesianRow = indonesianSheet.createRow(indonesianRowIdx++);
                indonesianRow.setHeightInPoints(60);
                Cell indonesianDateCell = indonesianRow.createCell(0);
                if (a.date != null) {
                    indonesianDateCell.setCellValue(java.sql.Date.valueOf(a.date));
                    indonesianDateCell.setCellStyle(dateStyle);
                } else {
                    indonesianDateCell.setCellValue("");
                }
                indonesianRow.createCell(1).setCellValue(a.source != null ? a.source : "");
                indonesianRow.createCell(2).setCellValue(a.title != null ? a.title : "");
                Cell idSummaryCell = indonesianRow.createCell(3);
                idSummaryCell.setCellValue(a.idSummary != null ? a.idSummary : "");
                idSummaryCell.setCellStyle(wrapStyle);
                Cell contentCell = indonesianRow.createCell(4);
                contentCell.setCellValue(a.content != null ? a.content : "");
                contentCell.setCellStyle(wrapStyle);
                Cell indonesianLinkCell = indonesianRow.createCell(5);
                if (a.link != null && !a.link.isBlank()) {
                    indonesianLinkCell.setCellValue(a.link);
                    indonesianLinkCell.setCellStyle(linkStyle);
                    var link = creationHelper.createHyperlink(HyperlinkType.URL);
                    link.setAddress(a.link);
                    indonesianLinkCell.setHyperlink(link);
                } else {
                    indonesianLinkCell.setCellValue("");
                }
            }

            int[] koreanWidths = new int[] { 12, 18, 16, 40, 60, 80, 120, 80, 60, 10 };
            for (int i = 0; i < koreanWidths.length; i++) {
                koreanSheet.setColumnWidth(i, koreanWidths[i] * 256);
            }

            int[] indonesianWidths = new int[] { 12, 18, 60, 80, 120, 60 };
            for (int i = 0; i < indonesianWidths.length; i++) {
                indonesianSheet.setColumnWidth(i, indonesianWidths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("엑셀 생성에 실패했습니다.", e);
        }
    }

    public List<ArticleDto> listScrappedArticles(Long userId) {
        String sql = """
            SELECT
                p.id,
                p.raw_news_id,
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                r.img,
                p.kor_title,
                p.kor_summary,
                p.id_summary,
                p.semantic_confidence,
                p.tag_mismatch,
                p.category_mismatch,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.insight,
                p.tags
            FROM user_scrap s
            JOIN processed_news p ON p.raw_news_id = s.raw_news_id
            JOIN raw_news r ON r.id = p.raw_news_id
            WHERE s.user_id = ?
              AND p.is_pharma_related IS TRUE
            ORDER BY s.created_at DESC, p.id DESC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> toDto(rs), userId);
    }

    private ArticleDto toDto(ResultSet rs) throws SQLException {
        ArticleDto dto = new ArticleDto();
        Object idObj = rs.getObject("id");
        dto.id = idObj == null ? null : ((Number) idObj).longValue();
        Object rawIdObj = rs.getObject("raw_news_id");
        dto.rawNewsId = rawIdObj == null ? null : ((Number) rawIdObj).longValue();
        dto.title = rs.getString("title");
        dto.korTitle = rs.getString("kor_title");
        dto.engTitle = null;
        dto.link = rs.getString("link");
        dto.content = rs.getString("content");
        dto.date = rs.getDate("published_date") != null
            ? rs.getDate("published_date").toLocalDate()
            : null;
        dto.source = rs.getString("source");
        dto.img = rs.getString("img");
        dto.category = rs.getString("category");
        dto.engCategory = rs.getString("eng_category");
        dto.korSummary = rs.getString("kor_summary");
        dto.engSummary = null;
        dto.idSummary = rs.getString("id_summary");
        Object confObj = rs.getObject("semantic_confidence");
        dto.semanticConfidence = confObj == null ? null : ((Number) confObj).floatValue();
        dto.tagMismatch = (Boolean) rs.getObject("tag_mismatch");
        dto.categoryMismatch = (Boolean) rs.getObject("category_mismatch");
        dto.korContent = rs.getString("kor_content");
        dto.importance = rs.getObject("importance") == null ? null : rs.getInt("importance");
        dto.insight = rs.getString("insight");

        String tagsJson = rs.getString("tags");
        dto.tags = parseTags(tagsJson);
        return dto;
    }

    private List<KeywordDto> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            List<?> raw = objectMapper.readValue(tagsJson, List.class);
            List<KeywordDto> out = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof String s && !s.isBlank()) {
                    KeywordDto k = new KeywordDto();
                    k.name = s;
                    out.add(k);
                } else if (item instanceof java.util.Map<?, ?> m) {
                    Object name = m.get("name");
                    if (name instanceof String s && !s.isBlank()) {
                        KeywordDto k = new KeywordDto();
                        k.name = s;
                        out.add(k);
                    }
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

}
