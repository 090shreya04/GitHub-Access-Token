package com.yourname.githubreport.service;

import com.yourname.githubreport.model.AccessReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class ReportService {

    @Autowired
    private GitHubService githubService;

    public Mono<AccessReport> generateAccessReport(String orgName) {
        return githubService.getFullAccessMapping(orgName)
                .map(mappings -> AccessReport.builder()
                        .organization(orgName)
                        .userAccessMappings(mappings)
                        .totalUsers(mappings.size())
                        .totalRepositories((int) mappings.stream()
                                .flatMap(m -> m.getRepositories().stream())
                                .map(r -> r.getFullName())
                                .distinct()
                                .count())
                        .generatedAt(LocalDateTime.now())
                        .build());
    }
}
