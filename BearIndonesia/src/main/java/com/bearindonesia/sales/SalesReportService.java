package com.bearindonesia.sales;

import com.bearindonesia.auth.AuthUser;
import java.io.BufferedInputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SalesReportService {

    private static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;

    private final SalesReportRepository repository;
    private final SalesReportExcelParser parser;
    private final SalesReportHtmlRenderer renderer;

    public SalesReportService(SalesReportRepository repository, SalesReportExcelParser parser, SalesReportHtmlRenderer renderer) {
        this.repository = repository;
        this.parser = parser;
        this.renderer = renderer;
    }

    public List<SalesReportListResponse> list(AuthUser admin) {
        List<SalesReportListItem> items = repository.listReports();
        return items.stream().map(i -> new SalesReportListResponse(
                i.id(),
                i.title(),
                i.originalFilename(),
                new SalesReportCreatedBy(i.createdById(), i.createdByName(), i.createdByEmail()),
                i.createdAt(),
                i.updatedAt()
        )).toList();
    }

    public long create(AuthUser admin, String title, MultipartFile file) {
        validateFile(file);
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.xlsx");
        if (!originalFilename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("xlsx 파일만 업로드할 수 있습니다.");
        }

        try (var in = new BufferedInputStream(file.getInputStream())) {
            SalesReportData data = parser.parse(in);
            OffsetDateTime now = OffsetDateTime.now();
            String html = renderer.render(title, originalFilename, new SalesReportCreatedBy(admin.id(), admin.name(), admin.email()), now, data);
            byte[] bytes = file.getBytes();
            return repository.create(title, originalFilename, bytes, html, admin.id());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("엑셀 처리에 실패했습니다: " + e.getMessage(), e);
        }
    }

    public String getHtml(long id) {
        return repository.findHtml(id)
                .filter(s -> s != null && !s.isBlank())
                .orElse(null);
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("파일 크기는 최대 20MB까지 가능합니다.");
        }
    }
}

