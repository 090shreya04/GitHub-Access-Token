package com.yourname.githubreport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a GitHub repository returned by the GitHub API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Repository {
    private Long id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;
}
