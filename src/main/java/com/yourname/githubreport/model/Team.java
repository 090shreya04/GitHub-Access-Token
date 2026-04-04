package com.yourname.githubreport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    private Long id;
    private String slug;
    private String name;
    private String permission; // pull, push, admin, maintain, triage
}
