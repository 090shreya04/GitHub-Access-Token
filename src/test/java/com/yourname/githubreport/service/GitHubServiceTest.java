package com.yourname.githubreport.service;

import com.yourname.githubreport.client.GitHubApiClient;
import com.yourname.githubreport.model.Repository;
import com.yourname.githubreport.model.User;
import com.yourname.githubreport.model.UserAccessMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubServiceTest {

    @Mock
    private GitHubApiClient client;

    @InjectMocks
    private GitHubService githubService;

    @Test
    void shouldAggregateUsersToRepositories() {
        // Arrange
        Repository repo1 = Repository.builder().id(1L).name("repo1").fullName("myorg/repo1").build();
        User user1 = User.builder().login("alice").id(10L).build();
        User user2 = User.builder().login("bob").id(20L).build();

        when(client.getOrgRepositories("myorg")).thenReturn(Flux.just(repo1));
        when(client.getRepoCollaborators("myorg", "repo1")).thenReturn(Flux.just(user1, user2));
        when(client.getRepoTeams("myorg", "repo1")).thenReturn(Flux.empty());

        // Act
        List<UserAccessMapping> result = githubService.getFullAccessMapping("myorg").block();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(m -> m.getUser().getLogin()))
                .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void shouldReturnEmptyListWhenOrgHasNoRepos() {
        // Arrange
        when(client.getOrgRepositories("emptyorg")).thenReturn(Flux.empty());

        // Act
        List<UserAccessMapping> result = githubService.getFullAccessMapping("emptyorg").block();

        // Assert
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void shouldSkipRepoWhenCollaboratorFetchFails() {
        // Arrange
        Repository repo1 = Repository.builder().id(1L).name("repo1").fullName("myorg/repo1").build();
        User user1 = User.builder().login("alice").id(10L).build();

        when(client.getOrgRepositories("myorg")).thenReturn(Flux.just(repo1));
        when(client.getRepoCollaborators("myorg", "repo1")).thenReturn(Flux.empty()); // Permission denied → empty
        when(client.getRepoContributors("myorg", "repo1")).thenReturn(Flux.just(user1)); // fallback works
        when(client.getRepoTeams("myorg", "repo1")).thenReturn(Flux.empty());

        // Act
        List<UserAccessMapping> result = githubService.getFullAccessMapping("myorg").block();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getLogin()).isEqualTo("alice");
        assertThat(result.get(0).getRepositories().get(0).getAccessType()).isEqualTo("direct");
    }
}
