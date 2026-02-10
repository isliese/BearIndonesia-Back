package com.bearindonesia.sales;

import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelValueReader {

    private final FormulaEvaluator evaluator;
    private final DataFormatter formatter;

    public ExcelValueReader(Workbook workbook) {
        this.evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        this.formatter = new DataFormatter(Locale.US);
    }

    public String asString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    public Double asNumber(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA) {
                var cv = evaluator.evaluate(cell);
                if (cv == null) {
                    return null;
                }
                return switch (cv.getCellType()) {
                    case NUMERIC -> cv.getNumberValue();
                    case STRING -> parseNumber(cv.getStringValue());
                    case BOOLEAN -> cv.getBooleanValue() ? 1.0 : 0.0;
                    default -> null;
                };
            }
            if (type == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
            if (type == CellType.STRING) {
                return parseNumber(cell.getStringCellValue());
            }
            if (type == CellType.BOOLEAN) {
                return cell.getBooleanCellValue() ? 1.0 : 0.0;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Cell findLabelCell(Sheet sheet, String... labels) {
        if (sheet == null || labels == null || labels.length == 0) {
            return null;
        }
        int lastRow = Math.min(sheet.getLastRowNum(), 500);
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            short lastCell = row.getLastCellNum();
            int maxCell = Math.min((int) lastCell, 80);
            for (int c = 0; c < maxCell; c++) {
                Cell cell = row.getCell(c);
                String v = asString(cell);
                if (v.isBlank()) {
                    continue;
                }
                for (String label : labels) {
                    if (label != null && !label.isBlank() && equalsNormalized(v, label)) {
                        return cell;
                    }
                }
            }
        }
        return null;
    }

    public Cell rightOf(Cell cell) {
        if (cell == null) {
            return null;
        }
        Row row = cell.getRow();
        return row == null ? null : row.getCell(cell.getColumnIndex() + 1);
    }

    private static boolean equalsNormalized(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static Double parseNumber(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isBlank()) {
            return null;
        }
        // Remove currency symbols and thousand separators.
        t = t.replaceAll("[^0-9eE+\\-\\.]", "");
        if (t.isBlank() || t.equals("-") || t.equals(".")) {
            return null;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

