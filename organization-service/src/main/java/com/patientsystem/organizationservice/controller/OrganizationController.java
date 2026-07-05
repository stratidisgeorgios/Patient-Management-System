package com.patientsystem.organizationservice.controller;

import com.patientsystem.organizationservice.dto.OrganizationRequestDTO;
import com.patientsystem.organizationservice.dto.OrganizationResponseDTO;
import com.patientsystem.organizationservice.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organization Controller", description = "APIs for managing organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @Operation(summary = "Create a new organization", description = "Create a new organization in the system. The request body must contain valid organization information.")
    public ResponseEntity<OrganizationResponseDTO> createOrganization(
            @Validated @RequestBody OrganizationRequestDTO organizationRequestDTO,
            HttpServletRequest request) {
        String sub = request.getHeader("X-User-Sub");
        OrganizationResponseDTO newOrganization = organizationService.createOrganization(organizationRequestDTO, sub);
        return ResponseEntity.ok(newOrganization);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID", description = "Retrieve a specific organization's information by their unique ID.")
    public ResponseEntity<OrganizationResponseDTO> getOrganizationById(@PathVariable UUID id) {
        OrganizationResponseDTO organization = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(organization);
    }
}
