package com.patientsystem.organizationservice.service;
import com.patientsystem.organizationservice.model.Organization;
import com.patientsystem.organizationservice.repository.OrganizationRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import com.patientsystem.organizationservice.dto.OrganizationRequestDTO;
import com.patientsystem.organizationservice.dto.OrganizationResponseDTO;
import com.patientsystem.organizationservice.mapper.OrganizationMapper;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final CognitoService cognitoService;

    public OrganizationService(OrganizationRepository organizationRepository, CognitoService cognitoService) {
        this.organizationRepository = organizationRepository;
        this.cognitoService = cognitoService;
    }

    public OrganizationResponseDTO createOrganization(OrganizationRequestDTO organizationRequestDTO, String sub) {
        Organization organization = OrganizationMapper.toModel(organizationRequestDTO);
        Organization savedOrganization = organizationRepository.save(organization);
        cognitoService.setOrganizationId(sub, savedOrganization.getId().toString());
        return OrganizationMapper.toDTO(savedOrganization);
    }

    public OrganizationResponseDTO getOrganizationById(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + organizationId));
        return OrganizationMapper.toDTO(organization);
    }
}
