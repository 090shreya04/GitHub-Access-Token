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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GitHubService — covers aggregation logic,
 * empty org handling, and collaborator fallback to contributors.
 */
@ExtendWith(MockitoExtension.class)
class GitHubServiceTest {

    @Mock
    private GitHubApiClient client;

    @InjectMocks
    private GitHubService githubService;

    @Test
    void shouldAggregateUsersToRepositories() {
        // Arrange: one repo with two collaborators
        Repository repo = Repository.builder().id(1L).name("repo1").fullName("myorg/repo1").build();
        User alice = User.builder().login("alice").id(10L).build();
        User bob = User.builder().login("bob").id(20L).build();

        when(client.getOrgRepositories("myorg")).thenReturn(Flux.just(repo));
        when(client.getRepoCollaborators("myorg", "repo1")).thenReturn(Flux.just(alice, bob));
        when(client.getRepoTeams("myorg", "repo1")).thenReturn(Flux.empty());

        // Act
        List<UserAccessMapping> result = githubService.getFullAccessMapping("myorg").block();

        // Assert: both users should appear in the mapping
        assertThat(result).isNotNull().hasSize(2);
        assertThat(result.stream().map(m -> m.getUser().getLogin()))
                .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void shouldReturnEmptyListWhenOrgHasNoRepos() {
        // Arrange: org exists but has no repositories
        when(client.getOrgRepositories("emptyorg")).thenReturn(Flux.empty());

        // Act
        List<UserAccessMapping> result = githubService.getFullAccessMapping("emptyorg").block();

        // Assert
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void shouldFallbackToContributorsWhenCollaboratorsAreDenied() {
        // Arrange: collaborators returns empty (no admin token), contributors returns alice
        Repository repo = Repository.builder().id(1L).name("repo1").fullName("myorg/repo1").build();
        User alice = User.builder().login("alice").id(10L).build();

        when(client.getOrgRepositories("myorg")).thenReturn(Flux.just(repo));
        when(client.getRepoCollaborators("myorg", "repo1")).thenReturn(Flux.empty());
        when(client.getRepoContributors("myorg", "repo1")).thenReturn(Flux.just(alice));
        when(client.getRepoTeams("myorg", "repo1")).thenReturn(Flux.empty());

        // Act
        List<UserAccessMapping> result = githubService.getFullAccessMapping("myorg").block();

        // Assert: alice comes through via fallback with "direct" access type
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getLogin()).isEqualTo("alice");
        assertThat(result.get(0).getRepositories().get(0).getAccessType()).isEqualTo("direct");
    }
}
