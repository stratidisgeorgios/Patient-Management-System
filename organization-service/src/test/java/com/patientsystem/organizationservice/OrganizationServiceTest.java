package com.patientsystem.organizationservice;

import com.patientsystem.organizationservice.dto.OrganizationRequestDTO;
import com.patientsystem.organizationservice.dto.OrganizationResponseDTO;
import com.patientsystem.organizationservice.mapper.OrganizationMapper;
import com.patientsystem.organizationservice.model.Organization;
import com.patientsystem.organizationservice.repository.OrganizationRepository;
import com.patientsystem.organizationservice.service.CognitoService;
import com.patientsystem.organizationservice.service.OrganizationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    OrganizationRepository organizationRepository;
    @Mock
    CognitoService cognitoService;

    @InjectMocks
    OrganizationService organizationService;

    private Organization sampleOrg(UUID id) {
        Organization org = new Organization();
        org.setId(id);
        org.setName("Acme Clinic");
        org.setAdminEmail("admin@acme.com");
        org.setRegisteredDate(LocalDate.of(2024, 1, 1));
        return org;
    }

    @Test
    void createOrganization_savesAndSetsCognitoAttribute() {
        UUID id = UUID.randomUUID();
        Organization saved = sampleOrg(id);
        OrganizationRequestDTO req = new OrganizationRequestDTO();
        req.setName("Acme Clinic");
        req.setAdminEmail("admin@acme.com");

        when(organizationRepository.save(any(Organization.class))).thenReturn(saved);

        OrganizationResponseDTO result = organizationService.createOrganization(req, "cognito-sub-123");

        assertThat(result.getId()).isEqualTo(id.toString());
        verify(cognitoService).setOrganizationId("cognito-sub-123", id.toString());
    }

    @Test
    void getOrganizationById_returnsOrganization_whenFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.of(sampleOrg(id)));

        OrganizationResponseDTO result = organizationService.getOrganizationById(id);

        assertThat(result.getName()).isEqualTo("Acme Clinic");
    }

    @Test
    void getOrganizationById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getOrganizationById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
