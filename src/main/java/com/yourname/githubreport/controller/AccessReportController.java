package com.yourname.githubreport.controller;

import com.yourname.githubreport.model.AccessReport;
import com.yourname.githubreport.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reports")
public class AccessReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/access")
    public Mono<AccessReport> getAccessReport(@RequestParam String org) {
        return reportService.generateAccessReport(org);
    }
}
