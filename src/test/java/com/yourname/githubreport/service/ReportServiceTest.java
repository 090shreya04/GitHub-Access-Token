package com.yourname.githubreport.service;

import com.yourname.githubreport.client.GitHubApiClient;
import com.yourname.githubreport.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private GitHubApiClient client;

    @InjectMocks
    private GitHubService githubService;

    @Test
    void reportShouldContainMetadata() {
        // Arrange
        Repository repo1 = Repository.builder().id(1L).name("repo1").fullName("myorg/repo1").build();
        Repository repo2 = Repository.builder().id(2L).name("repo2").fullName("myorg/repo2").build();
        User user1 = User.builder().login("alice").id(10L).build();

        when(client.getOrgRepositories("myorg")).thenReturn(Flux.just(repo1, repo2));
        when(client.getRepoCollaborators("myorg", "repo1")).thenReturn(Flux.just(user1));
        when(client.getRepoCollaborators("myorg", "repo2")).thenReturn(Flux.just(user1));
        when(client.getRepoTeams("myorg", "repo1")).thenReturn(Flux.empty());
        when(client.getRepoTeams("myorg", "repo2")).thenReturn(Flux.empty());

        ReportService reportService = new ReportService();
        // Inject manually since Spring is not involved
        setField(reportService, "githubService", githubService);

        // Act
        var report = reportService.generateAccessReport("myorg").block();

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.getOrganization()).isEqualTo("myorg");
        assertThat(report.getTotalUsers()).isEqualTo(1);
        assertThat(report.getTotalRepositories()).isEqualTo(2);
        assertThat(report.getGeneratedAt()).isNotNull();
        assertThat(report.getGeneratedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // Helper to inject fields without Spring
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
