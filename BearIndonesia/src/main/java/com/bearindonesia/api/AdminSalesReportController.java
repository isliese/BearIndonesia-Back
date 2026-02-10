package com.bearindonesia.api;

import com.bearindonesia.auth.AuthUser;
import com.bearindonesia.auth.SecurityUtils;
import com.bearindonesia.sales.SalesReportIdResponse;
import com.bearindonesia.sales.SalesReportListResponse;
import com.bearindonesia.sales.SalesReportService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/admin/sales", "/api/admin/sales"})
public class AdminSalesReportController {

    private final SalesReportService service;

    public AdminSalesReportController(SalesReportService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    public List<SalesReportListResponse> listReports() {
        AuthUser admin = SecurityUtils.requireAdmin();
        return service.list(admin);
    }

    @PostMapping("/reports")
    public ResponseEntity<SalesReportIdResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) {
        AuthUser admin = SecurityUtils.requireAdmin();
        long id = service.create(admin, title, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SalesReportIdResponse(id));
    }

    @GetMapping("/reports/{id}/html")
    public ResponseEntity<?> previewHtml(@PathVariable("id") long id) {
        SecurityUtils.requireAdmin();
        String html = service.getHtml(id);
        if (html == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                    .body(new MessageResponse("not found"));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=utf-8"))
                .body(html);
    }
}
