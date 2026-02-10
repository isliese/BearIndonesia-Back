package com.bearindonesia.sales;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalesReportData {
    public Map<String, String> kpis = new LinkedHashMap<>();
    public List<Point> dailyNetSales = List.of();
    public List<Slice> salesByRpm = List.of();
    public List<Slice> salesByChannel = List.of();

    public record Point(String label, double value) {}
    public record Slice(String label, double value) {}
}

