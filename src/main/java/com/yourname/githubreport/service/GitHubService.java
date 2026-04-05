package com.yourname.githubreport.service;

import com.yourname.githubreport.client.GitHubApiClient;
import com.yourname.githubreport.model.Repository;
import com.yourname.githubreport.model.RepositoryAccess;
import com.yourname.githubreport.model.User;
import com.yourname.githubreport.model.UserAccessMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core service that builds a user-to-repository access map for a GitHub organization.
 * It fetches both direct (collaborator/contributor) and team-based access in parallel.
 */
@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    @Autowired
    private GitHubApiClient client;

    /**
     * Build a complete user → repositories access mapping for an organization.
     * Tries collaborators first (requires admin), then falls back to contributors (public).
     * Also fetches team-based access.
     */
    public Mono<List<UserAccessMapping>> getFullAccessMapping(String orgName) {
        log.info("Generating full access mapping for organization: {}", orgName);

        // user login → map of repoFullName → RepositoryAccess
        Map<String, Map<String, RepositoryAccess>> userRepoMap = new ConcurrentHashMap<>();

        return client.getOrgRepositories(orgName)
                .flatMap(repo -> {
                    log.debug("Processing repo: {}", repo.getName());
                    return Flux.merge(
                            // 1. Direct contributors/collaborators
                            getDirectAccess(orgName, repo, userRepoMap),
                            // 2. Team-based access
                            getTeamAccess(orgName, repo, userRepoMap)
                    );
                }, 5) // max 5 concurrent repo fetches
                .then(Mono.fromCallable(() -> buildMappings(userRepoMap)));
    }

    private Flux<Void> getDirectAccess(String orgName, Repository repo,
                                        Map<String, Map<String, RepositoryAccess>> userRepoMap) {
        // Try collaborators (requires org admin). Falls back to contributors.
        return client.getRepoCollaborators(orgName, repo.getName())
                .switchIfEmpty(Flux.defer(() -> client.getRepoContributors(orgName, repo.getName())))
                .doOnNext(user -> {
                    RepositoryAccess access = RepositoryAccess.builder()
                            .id(repo.getId())
                            .name(repo.getName())
                            .fullName(repo.getFullName())
                            .permission(extractPermission(user))
                            .accessType("direct")
                            .build();
                    userRepoMap
                            .computeIfAbsent(user.getLogin(), k -> new ConcurrentHashMap<>())
                            .put(repo.getFullName(), access);
                })
                .thenMany(Flux.empty());
    }

    private Flux<Void> getTeamAccess(String orgName, Repository repo,
                                      Map<String, Map<String, RepositoryAccess>> userRepoMap) {
        return client.getRepoTeams(orgName, repo.getName())
                .flatMap(team -> client.getTeamMembers(orgName, team.getSlug())
                        .doOnNext(user -> {
                            // Team access only adds if not already mapped with direct access
                            String repoKey = repo.getFullName();
                            Map<String, RepositoryAccess> userMap = userRepoMap
                                    .computeIfAbsent(user.getLogin(), k -> new ConcurrentHashMap<>());
                            userMap.putIfAbsent(repoKey, RepositoryAccess.builder()
                                    .id(repo.getId())
                                    .name(repo.getName())
                                    .fullName(repo.getFullName())
                                    .permission(team.getPermission())
                                    .accessType("team")
                                    .teamName(team.getName())
                                    .build());
                        })
                )
                .thenMany(Flux.empty());
    }

    private String extractPermission(User user) {
        // GitHub collaborator response includes permission if token has admin access
        // Falls back to "read" for contributors
        if (user.getPermissions() != null) {
            if (Boolean.TRUE.equals(user.getPermissions().get("admin"))) return "admin";
            if (Boolean.TRUE.equals(user.getPermissions().get("push"))) return "write";
            if (Boolean.TRUE.equals(user.getPermissions().get("pull"))) return "read";
        }
        return "read"; // contributors have at least read access
    }

    private List<UserAccessMapping> buildMappings(Map<String, Map<String, RepositoryAccess>> userRepoMap) {
        return userRepoMap.entrySet().stream()
                .map(entry -> {
                    User user = User.builder().login(entry.getKey()).build();
                    List<RepositoryAccess> repos = new ArrayList<>(entry.getValue().values());
                    return UserAccessMapping.builder()
                            .user(user)
                            .repositories(repos)
                            .build();
                })
                .sorted(Comparator.comparing(m -> m.getUser().getLogin()))
                .collect(Collectors.toList());
    }
}
