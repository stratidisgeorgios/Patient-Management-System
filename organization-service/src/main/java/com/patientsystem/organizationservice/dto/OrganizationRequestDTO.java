package com.patientsystem.organizationservice.dto;

public class OrganizationRequestDTO {
    private String name;
    private String adminEmail;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAdminEmail() {
        return adminEmail;
    }
    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

}
