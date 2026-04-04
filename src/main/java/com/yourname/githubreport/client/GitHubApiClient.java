package com.yourname.githubreport.client;

import com.yourname.githubreport.exception.GitHubApiException;
import com.yourname.githubreport.exception.OrganizationNotFoundException;
import com.yourname.githubreport.exception.RateLimitException;
import com.yourname.githubreport.model.Repository;
import com.yourname.githubreport.model.Team;
import com.yourname.githubreport.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);

    @Autowired
    private WebClient githubWebClient;

    /**
     * Fetch all repositories for a GitHub organization.
     */
    public Flux<Repository> getOrgRepositories(String orgName) {
        log.info("Fetching repositories for org: {}", orgName);
        return githubWebClient.get()
                .uri("/orgs/{org}/repos?per_page=100&type=all", orgName)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> Mono.error(new OrganizationNotFoundException(orgName)))
                .onStatus(status -> status.value() == 401,
                        response -> Mono.error(new GitHubApiException("Invalid or expired GitHub token. Please check your GITHUB_TOKEN.", 401)))
                .onStatus(status -> status.value() == 403,
                        response -> !response.headers().header("X-RateLimit-Remaining").isEmpty()
                            ? Mono.error(new RateLimitException("GitHub API rate limit exceeded. Please wait before retrying."))
                            : Mono.error(new GitHubApiException("Access forbidden. Token may not have required permissions.", 403)))
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new GitHubApiException(
                                        "GitHub API error: " + response.statusCode() + " - " + body,
                                        response.statusCode().value()))))
                .bodyToFlux(Repository.class);
    }

    /**
     * Fetch all collaborators (direct access) for a repository.
     * Returns empty on permission errors (403) — expected for non-admin tokens.
     */
    public Flux<User> getRepoCollaborators(String owner, String repoName) {
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/collaborators?per_page=100&affiliation=all", owner, repoName)
                .retrieve()
                .bodyToFlux(User.class)
                .doOnError(e -> log.debug("Could not fetch collaborators for {}/{}: {}", owner, repoName, e.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }

    /**
     * Fetch all contributors (public) for a repository.
     */
    public Flux<User> getRepoContributors(String owner, String repoName) {
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/contributors?per_page=100&anon=false", owner, repoName)
                .retrieve()
                .bodyToFlux(User.class)
                .doOnError(e -> log.debug("Could not fetch contributors for {}/{}: {}", owner, repoName, e.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }

    /**
     * Fetch all teams for a repository.
     * Requires org admin token — returns empty if no permission.
     */
    public Flux<Team> getRepoTeams(String owner, String repoName) {
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/teams?per_page=100", owner, repoName)
                .retrieve()
                .bodyToFlux(Team.class)
                .doOnError(e -> log.debug("Could not fetch teams for {}/{}: {}", owner, repoName, e.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }

    /**
     * Fetch all members of a team.
     */
    public Flux<User> getTeamMembers(String org, String teamSlug) {
        return githubWebClient.get()
                .uri("/orgs/{org}/teams/{team_slug}/members?per_page=100", org, teamSlug)
                .retrieve()
                .bodyToFlux(User.class)
                .doOnError(e -> log.debug("Could not fetch members for team {}/{}: {}", org, teamSlug, e.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }
}
