package com.patientsystem.organizationservice.service;
import com.patientsystem.organizationservice.model.Organization;
import com.patientsystem.organizationservice.repository.OrganizationRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import com.patientsystem.organizationservice.grpc.PatientSchemaGrpcClient;
import com.patientsystem.organizationservice.grpc.TreatmentSchemaGrpcClient;
import com.patientsystem.organizationservice.grpc.BillingSchemaGrpcClient;
import com.patientsystem.organizationservice.grpc.AnalyticsSchemaGrpcClient;
import com.patientsystem.organizationservice.dto.OrganizationRequestDTO;
import com.patientsystem.organizationservice.dto.OrganizationResponseDTO;
import com.patientsystem.organizationservice.mapper.OrganizationMapper;
@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final PatientSchemaGrpcClient patientSchemaGrpcClient;
    private final TreatmentSchemaGrpcClient treatmentSchemaGrpcClient;
    private final BillingSchemaGrpcClient billingSchemaGrpcClient;
    private final AnalyticsSchemaGrpcClient analyticsSchemaGrpcClient;
    private final CognitoService cognitoService;

    public OrganizationService(OrganizationRepository organizationRepository, PatientSchemaGrpcClient patientSchemaGrpcClient, TreatmentSchemaGrpcClient treatmentSchemaGrpcClient, BillingSchemaGrpcClient billingSchemaGrpcClient, AnalyticsSchemaGrpcClient analyticsSchemaGrpcClient, CognitoService cognitoService) {
        this.organizationRepository = organizationRepository;
        this.patientSchemaGrpcClient = patientSchemaGrpcClient;
        this.treatmentSchemaGrpcClient = treatmentSchemaGrpcClient;
        this.billingSchemaGrpcClient = billingSchemaGrpcClient;
        this.analyticsSchemaGrpcClient = analyticsSchemaGrpcClient;
        this.cognitoService = cognitoService;
    }

    public OrganizationResponseDTO createOrganization(OrganizationRequestDTO organizationRequestDTO, String sub) {
        Organization organization = OrganizationMapper.toModel(organizationRequestDTO);
        Organization savedOrganization = organizationRepository.save(organization);
        patientSchemaGrpcClient.provisionSchema(savedOrganization.getId().toString());
        treatmentSchemaGrpcClient.provisionSchema(savedOrganization.getId().toString());
        billingSchemaGrpcClient.provisionSchema(savedOrganization.getId().toString());
        analyticsSchemaGrpcClient.provisionSchema(savedOrganization.getId().toString());
        cognitoService.setOrganizationId(sub, savedOrganization.getId().toString());
        return OrganizationMapper.toDTO(savedOrganization);
    }

    public OrganizationResponseDTO getOrganizationById(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + organizationId));
        return OrganizationMapper.toDTO(organization);
    }
}
