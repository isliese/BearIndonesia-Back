package com.bearindonesia.sales;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class SalesReportExcelParser {

    public SalesReportData parse(InputStream in) {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet summary = wb.getSheet("Executive Summary");
            if (summary == null) {
                summary = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            }
            if (summary == null) {
                throw new IllegalArgumentException("엑셀 시트를 찾을 수 없습니다.");
            }

            ExcelValueReader r = new ExcelValueReader(wb);
            SalesReportData out = new SalesReportData();

            readKpi(out, r, summary, "Total Sales", "Total Sales (Box)", "Total Sales(Box)");
            readKpi(out, r, summary, "Gross Sales", "Gross Sales (IDR)");
            readKpi(out, r, summary, "Net Sales", "Net Sales (IDR)");
            readKpi(out, r, summary, "Achievement", "Achievement (%)");
            readKpi(out, r, summary, "Avg Discount", "Avg Discount (%)", "Average Discount (%)");
            readKpi(out, r, summary, "Active Days", "Active Days (days)");

            out.dailyNetSales = readTwoColumnTable(
                    r,
                    summary,
                    new String[] {"day", "date"},
                    new String[] {"net sales", "net sales (idr)", "daily net sales"}
            );
            out.salesByRpm = readSlices(
                    r,
                    summary,
                    new String[] {"rpm", "rpm group"},
                    new String[] {"net sales", "net sales (idr)"}
            );
            out.salesByChannel = readSlices(
                    r,
                    summary,
                    new String[] {"channel"},
                    new String[] {"net sales", "net sales (idr)"}
            );

            // Minimum required: KPIs must exist, otherwise likely wrong template.
            if (out.kpis.isEmpty()) {
                throw new IllegalArgumentException("지원되지 않는 엑셀 템플릿입니다. (KPI를 찾지 못했습니다.)");
            }
            return out;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("엑셀 파싱에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private static void readKpi(SalesReportData out, ExcelValueReader r, Sheet sheet, String... labels) {
        Cell label = r.findLabelCell(sheet, labels);
        if (label == null) {
            return;
        }
        Cell valueCell = r.rightOf(label);
        String value = r.asString(valueCell);
        if (!value.isBlank()) {
            out.kpis.put(labels[0], value);
        }
    }

    private static List<SalesReportData.Point> readTwoColumnTable(
            ExcelValueReader r,
            Sheet sheet,
            String[] xHeaders,
            String[] yHeaders
    ) {
        TableHeader header = findHeaderRow(r, sheet, xHeaders, yHeaders);
        if (header == null) {
            return List.of();
        }
        List<SalesReportData.Point> points = new ArrayList<>();
        int maxRows = Math.min(sheet.getLastRowNum(), header.rowIndex + 60);
        for (int i = header.rowIndex + 1; i <= maxRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String label = r.asString(row.getCell(header.xCol));
            if (label.isBlank()) {
                if (!points.isEmpty()) {
                    break;
                }
                continue;
            }
            Double v = r.asNumber(row.getCell(header.yCol));
            if (v == null) {
                continue;
            }
            points.add(new SalesReportData.Point(label, v));
        }
        return points;
    }

    private static List<SalesReportData.Slice> readSlices(
            ExcelValueReader r,
            Sheet sheet,
            String[] labelHeaders,
            String[] valueHeaders
    ) {
        TableHeader header = findHeaderRow(r, sheet, labelHeaders, valueHeaders);
        if (header == null) {
            return List.of();
        }
        List<SalesReportData.Slice> slices = new ArrayList<>();
        int maxRows = Math.min(sheet.getLastRowNum(), header.rowIndex + 60);
        for (int i = header.rowIndex + 1; i <= maxRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String label = r.asString(row.getCell(header.xCol));
            if (label.isBlank()) {
                if (!slices.isEmpty()) {
                    break;
                }
                continue;
            }
            Double v = r.asNumber(row.getCell(header.yCol));
            if (v == null) {
                continue;
            }
            slices.add(new SalesReportData.Slice(label, v));
        }
        return slices;
    }

    private static TableHeader findHeaderRow(
            ExcelValueReader r,
            Sheet sheet,
            String[] xHeaders,
            String[] yHeaders
    ) {
        int lastRow = Math.min(sheet.getLastRowNum(), 300);
        for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            short last = row.getLastCellNum();
            int max = Math.min((int) last, 80);
            int xCol = -1;
            int yCol = -1;
            for (int c = 0; c < max; c++) {
                String v = r.asString(row.getCell(c));
                if (v.isBlank()) {
                    continue;
                }
                if (xCol < 0 && matchesAny(v, xHeaders)) {
                    xCol = c;
                    continue;
                }
                if (yCol < 0 && matchesAny(v, yHeaders)) {
                    yCol = c;
                }
            }
            if (xCol >= 0 && yCol >= 0) {
                return new TableHeader(rowIndex, xCol, yCol);
            }
        }
        return null;
    }

    private static boolean matchesAny(String value, String[] candidates) {
        if (candidates == null) {
            return false;
        }
        String norm = value.trim().toLowerCase();
        for (String c : candidates) {
            if (c == null) {
                continue;
            }
            if (norm.equals(c.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private record TableHeader(int rowIndex, int xCol, int yCol) {}
}

