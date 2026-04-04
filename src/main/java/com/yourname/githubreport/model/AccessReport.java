package com.yourname.githubreport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessReport {
    private String organization;
    private List<UserAccessMapping> userAccessMappings;
    private Integer totalUsers;
    private Integer totalRepositories;
    private LocalDateTime generatedAt;
}
