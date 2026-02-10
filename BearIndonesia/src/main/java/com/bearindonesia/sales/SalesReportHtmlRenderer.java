package com.bearindonesia.sales;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SalesReportHtmlRenderer {

    public String render(String title, String originalFilename, SalesReportCreatedBy createdBy, OffsetDateTime createdAt, SalesReportData data) {
        String safeTitle = escape(title == null || title.isBlank() ? originalFilename : title);
        String meta = escape(originalFilename)
                + " · "
                + escape(createdBy.name())
                + " · "
                + escape(createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        String kpiCards = renderKpis(data);
        String daily = renderBarChart("Daily Net Sales", data.dailyNetSales);
        String rpm = renderDonut("Sales by RPM", data.salesByRpm);
        String channel = renderDonut("Sales by Channel", data.salesByChannel);

        return """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>%s</title>
              <style>
                :root{--bg:#0b1220;--card:#0f1a2e;--muted:#9aa7bd;--text:#e8eefc;--border:rgba(255,255,255,.08);--accent:#6ea8fe;--good:#35d39e;}
                body{margin:0;padding:18px;background:linear-gradient(180deg,#070c16,#0b1220 40%%);color:var(--text);font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;}
                .container{max-width:1100px;margin:0 auto;}
                .header{display:flex;flex-direction:column;gap:6px;margin-bottom:14px;}
                .title{font-size:18px;font-weight:700;letter-spacing:.2px;}
                .meta{font-size:12px;color:var(--muted);}
                .grid{display:grid;grid-template-columns:repeat(12,1fr);gap:12px;}
                .card{background:rgba(15,26,46,.92);border:1px solid var(--border);border-radius:14px;padding:12px;}
                .kpis{grid-column:1/-1;display:grid;grid-template-columns:repeat(6,1fr);gap:10px;}
                .kpi{background:rgba(255,255,255,.03);border:1px solid var(--border);border-radius:12px;padding:10px;}
                .kpi .label{font-size:11px;color:var(--muted);margin-bottom:6px;}
                .kpi .value{font-size:16px;font-weight:700;}
                .section-title{font-size:13px;font-weight:700;margin:0 0 10px;}
                .chart{grid-column:1/-1;}
                .chart-row{display:grid;grid-template-columns:repeat(12,1fr);gap:12px;}
                .chart-row .half{grid-column:span 6;}
                svg{width:100%%;height:auto;display:block;}
                .legend{margin-top:10px;display:flex;flex-wrap:wrap;gap:8px 12px;font-size:12px;color:var(--muted);}
                .dot{display:inline-block;width:10px;height:10px;border-radius:999px;margin-right:6px;vertical-align:-1px;}
                @media (max-width: 980px){
                  .kpis{grid-template-columns:repeat(2,1fr);}
                  .chart-row .half{grid-column:1/-1;}
                }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <div class="title">%s</div>
                  <div class="meta">%s</div>
                </div>

                <div class="grid">
                  <div class="card kpis">%s</div>
                </div>

                <div style="height:12px"></div>

                <div class="grid">
                  <div class="card chart">
                    <div class="section-title">Daily Net Sales</div>
                    %s
                  </div>
                </div>

                <div style="height:12px"></div>

                <div class="chart-row">
                  <div class="card half">
                    <div class="section-title">Sales by RPM</div>
                    %s
                  </div>
                  <div class="card half">
                    <div class="section-title">Sales by Channel</div>
                    %s
                  </div>
                </div>
              </div>
            </body>
            </html>
            """.formatted(safeTitle, safeTitle, meta, kpiCards, daily, rpm, channel);
    }

    private String renderKpis(SalesReportData data) {
        if (data == null || data.kpis == null || data.kpis.isEmpty()) {
            return kpi("KPI", "N/A");
        }
        StringBuilder out = new StringBuilder();
        data.kpis.forEach((k, v) -> out.append(kpi(k, v)));
        return out.toString();
    }

    private static String kpi(String label, String value) {
        return """
            <div class="kpi">
              <div class="label">%s</div>
              <div class="value">%s</div>
            </div>
            """.formatted(escape(label), escape(value));
    }

    private String renderBarChart(String title, List<SalesReportData.Point> points) {
        if (points == null || points.isEmpty()) {
            return "<div class=\"meta\">No data</div>";
        }
        int w = 980;
        int h = 260;
        int paddingL = 46;
        int paddingR = 12;
        int paddingT = 10;
        int paddingB = 34;
        double max = points.stream().mapToDouble(SalesReportData.Point::value).max().orElse(1.0);
        max = max <= 0 ? 1.0 : max;
        int n = Math.min(points.size(), 31);
        double plotW = (w - paddingL - paddingR);
        double plotH = (h - paddingT - paddingB);
        double gap = 6;
        double barW = (plotW - gap * (n - 1)) / n;

        StringBuilder bars = new StringBuilder();
        for (int i = 0; i < n; i++) {
            SalesReportData.Point p = points.get(i);
            double x = paddingL + i * (barW + gap);
            double bh = (p.value() / max) * plotH;
            double y = paddingT + (plotH - bh);
            bars.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"")
                    .append(barW).append("\" height=\"").append(bh)
                    .append("\" rx=\"4\" fill=\"#6ea8fe\" fill-opacity=\"0.9\"/>");
        }

        // x labels: show every 3rd
        StringBuilder xLabels = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i % 3 != 0) continue;
            SalesReportData.Point p = points.get(i);
            double x = paddingL + i * (barW + gap) + barW / 2;
            xLabels.append("<text x=\"").append(x).append("\" y=\"").append(h - 14)
                    .append("\" text-anchor=\"middle\" font-size=\"11\" fill=\"#9aa7bd\">")
                    .append(escape(p.label())).append("</text>");
        }

        // y max label only
        String yMax = formatCompact(max);

        return """
            <svg viewBox="0 0 %d %d" role="img" aria-label="%s">
              <text x="0" y="18" font-size="11" fill="#9aa7bd">%s</text>
              <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="rgba(255,255,255,.12)"/>
              <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="rgba(255,255,255,.12)"/>
              <text x="%d" y="%d" font-size="11" fill="#9aa7bd">%s</text>
              %s
              %s
            </svg>
            """.formatted(
                w, h,
                escape(title),
                escape(yMax),
                paddingL, paddingT, w - paddingR, paddingT,
                paddingL, h - paddingB, w - paddingR, h - paddingB,
                6, paddingT + 10, escape(yMax),
                bars,
                xLabels
        );
    }

    private String renderDonut(String title, List<SalesReportData.Slice> slices) {
        if (slices == null || slices.isEmpty()) {
            return "<div class=\"meta\">No data</div>";
        }
        double total = slices.stream().mapToDouble(SalesReportData.Slice::value).sum();
        if (total <= 0) {
            return "<div class=\"meta\">No data</div>";
        }
        int w = 420;
        int h = 260;
        double cx = 130;
        double cy = 130;
        double rOuter = 92;
        double rInner = 56;

        String[] palette = new String[] {"#6ea8fe", "#35d39e", "#f59e0b", "#a78bfa", "#f472b6", "#22c55e", "#e879f9"};

        double angle = -Math.PI / 2;
        StringBuilder paths = new StringBuilder();
        StringBuilder legend = new StringBuilder("<div class=\"legend\">");
        for (int i = 0; i < Math.min(slices.size(), 7); i++) {
            SalesReportData.Slice s = slices.get(i);
            double frac = s.value() / total;
            double a2 = angle + frac * 2 * Math.PI;
            String color = palette[i % palette.length];
            paths.append(ringSlice(cx, cy, rOuter, rInner, angle, a2, color));
            legend.append("<span><span class=\"dot\" style=\"background:")
                    .append(color)
                    .append("\"></span>")
                    .append(escape(s.label()))
                    .append(" (")
                    .append((int) Math.round(frac * 100))
                    .append("%)</span>");
            angle = a2;
        }
        legend.append("</div>");

        String center = formatCompact(total);
        String svg = """
            <svg viewBox="0 0 %d %d" role="img" aria-label="%s">
              %s
              <circle cx="%.1f" cy="%.1f" r="%.1f" fill="rgba(255,255,255,.04)"/>
              <text x="%.1f" y="%.1f" text-anchor="middle" font-size="12" fill="#9aa7bd">Total</text>
              <text x="%.1f" y="%.1f" text-anchor="middle" font-size="16" font-weight="700" fill="#e8eefc">%s</text>
            </svg>
            """.formatted(
                w, h, escape(title),
                paths,
                cx, cy, rInner - 6,
                cx, cy - 4,
                cx, cy + 18,
                escape(center)
        );

        return svg + legend;
    }

    private static String ringSlice(double cx, double cy, double rOuter, double rInner, double a1, double a2, String color) {
        double x1o = cx + rOuter * Math.cos(a1);
        double y1o = cy + rOuter * Math.sin(a1);
        double x2o = cx + rOuter * Math.cos(a2);
        double y2o = cy + rOuter * Math.sin(a2);

        double x1i = cx + rInner * Math.cos(a1);
        double y1i = cy + rInner * Math.sin(a1);
        double x2i = cx + rInner * Math.cos(a2);
        double y2i = cy + rInner * Math.sin(a2);

        int large = (a2 - a1) > Math.PI ? 1 : 0;
        return """
            <path d="M %.2f %.2f A %.2f %.2f 0 %d 1 %.2f %.2f L %.2f %.2f A %.2f %.2f 0 %d 0 %.2f %.2f Z"
                  fill="%s" fill-opacity="0.92"/>
            """.formatted(x1o, y1o, rOuter, rOuter, large, x2o, y2o, x2i, y2i, rInner, rInner, large, x1i, y1i, color);
    }

    private static String formatCompact(double v) {
        double abs = Math.abs(v);
        if (abs >= 1_000_000_000) return String.format("%.1fB", v / 1_000_000_000.0);
        if (abs >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (abs >= 1_000) return String.format("%.1fK", v / 1_000.0);
        if (abs >= 100) return String.format("%.0f", v);
        return String.format("%.2f", v);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

