package com.back.domain.report.report.controller;

import com.back.domain.report.report.common.ReportType;
import com.back.domain.report.report.dto.ReportResBody;
import com.back.domain.report.report.service.ReportService;
import com.back.standard.util.page.PagePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/reports")
public class ReportAdminController {

    private final ReportService reportService;

    //TODO : Only Admin 권한
    @GetMapping
    public ResponseEntity<PagePayload<ReportResBody>> getReports(@PageableDefault(sort = "id", direction = DESC) Pageable pageable,
                                                                 @RequestParam(value = "reportType", required = false) ReportType reportType) {
        PagePayload<ReportResBody> response = reportService.getReports(pageable, reportType);
        return ResponseEntity.ok(response);
    }
}
