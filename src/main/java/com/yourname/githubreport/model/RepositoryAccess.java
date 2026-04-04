package com.yourname.githubreport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryAccess {
    private Long id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    /**
     * Permission level: admin, write, read
     */
    private String permission;

    /**
     * How user got access: "direct" (collaborator) or "team"
     */
    private String accessType;

    /**
     * Team name if accessType = "team"
     */
    private String teamName;
}
