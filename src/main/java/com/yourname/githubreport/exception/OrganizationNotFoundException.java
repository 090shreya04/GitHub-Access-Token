package com.yourname.githubreport.exception;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(String orgName) {
        super("Organization not found: " + orgName);
    }
}
