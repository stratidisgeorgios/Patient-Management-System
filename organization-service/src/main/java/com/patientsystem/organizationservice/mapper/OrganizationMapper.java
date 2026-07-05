package com.patientsystem.organizationservice.mapper;

import java.time.LocalDate;
import com.patientsystem.organizationservice.dto.OrganizationRequestDTO;
import com.patientsystem.organizationservice.dto.OrganizationResponseDTO;
import com.patientsystem.organizationservice.model.Organization;

public class OrganizationMapper {
    public static OrganizationResponseDTO toDTO(Organization organization){
        OrganizationResponseDTO organizationResponseDTO = new OrganizationResponseDTO();
        organizationResponseDTO.setId(organization.getId().toString());
        organizationResponseDTO.setName(organization.getName());
        organizationResponseDTO.setAdminEmail(organization.getAdminEmail());
        organizationResponseDTO.setRegisteredDate(organization.getRegisteredDate().toString());
        return organizationResponseDTO;
    }

    public static Organization toModel(OrganizationRequestDTO organizationRequestDTO){
        Organization organization = new Organization();
        organization.setName(organizationRequestDTO.getName());
        organization.setAdminEmail(organizationRequestDTO.getAdminEmail());
        organization.setRegisteredDate(LocalDate.now());
        return organization;
    }
}



