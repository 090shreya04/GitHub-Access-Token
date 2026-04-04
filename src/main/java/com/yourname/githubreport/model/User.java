package com.yourname.githubreport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String login;
    private Long id;

    /**
     * Present only when token has admin access (collaborators API).
     * Keys: "admin", "push", "pull" — values: true/false
     */
    @JsonProperty("permissions")
    private Map<String, Boolean> permissions;
}
