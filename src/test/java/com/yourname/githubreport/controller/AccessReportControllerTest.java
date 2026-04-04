package com.yourname.githubreport.controller;

import com.yourname.githubreport.model.AccessReport;
import com.yourname.githubreport.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {AccessReportController.class, GlobalExceptionHandler.class})
class AccessReportControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReportService reportService;

    @Test
    void shouldReturn200AndJsonForValidOrg() {
        AccessReport mockReport = AccessReport.builder()
                .organization("myorg")
                .userAccessMappings(List.of())
                .totalUsers(0)
                .totalRepositories(0)
                .generatedAt(LocalDateTime.now())
                .build();

        when(reportService.generateAccessReport("myorg")).thenReturn(Mono.just(mockReport));

        webTestClient.get().uri("/api/reports/access?org=myorg")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.organization").isEqualTo("myorg")
                .jsonPath("$.totalUsers").isEqualTo(0)
                .jsonPath("$.userAccessMappings").isArray();
    }

    @Test
    void shouldReturn400WhenOrgParamMissing() {
        webTestClient.get().uri("/api/reports/access")
                .exchange()
                .expectStatus().is4xxClientError();
    }
}
