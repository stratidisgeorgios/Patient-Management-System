package com.patientsystem.patientservice;

import com.patientsystem.patientservice.dto.PatientRequestDTO;
import com.patientsystem.patientservice.dto.PatientResponseDTO;
import com.patientsystem.patientservice.exception.EmailAlreadyExistsException;
import com.patientsystem.patientservice.exception.IdNotFoundException;
import com.patientsystem.patientservice.grpc.BillingServiceGrpcClient;
import com.patientsystem.patientservice.kafka.KafkaProducer;
import com.patientsystem.patientservice.model.Gender;
import com.patientsystem.patientservice.model.Patient;
import com.patientsystem.patientservice.repository.PatientRepository;
import com.patientsystem.patientservice.service.PatientService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    PatientRepository patientRepository;
    @Mock
    BillingServiceGrpcClient billingServiceGrpcClient;
    @Mock
    KafkaProducer kafkaProducer;

    @InjectMocks
    PatientService patientService;

    private Patient samplePatient(UUID id, String orgId) {
        Patient p = new Patient();
        p.setId(id);
        p.setName("Alice Smith");
        p.setEmail("alice@example.com");
        p.setGender(Gender.FEMALE);
        p.setAddress("123 Main St");
        p.setDateOfBirth(LocalDate.of(1990, 1, 15));
        p.setRegisteredDate(LocalDate.of(2024, 1, 1));
        p.setOrganizationId(orgId);
        return p;
    }

    private PatientRequestDTO sampleRequest() {
        PatientRequestDTO dto = new PatientRequestDTO();
        dto.setName("Alice Smith");
        dto.setEmail("alice@example.com");
        dto.setGender("FEMALE");
        dto.setAddress("123 Main St");
        dto.setDateOfBirth("1990-01-15");
        dto.setRegisteredDate("2024-01-01");
        return dto;
    }

    @Test
    void getPatientById_returnsPatient_whenFound() {
        UUID id = UUID.randomUUID();
        String orgId = "org-1";
        Patient patient = samplePatient(id, orgId);
        when(patientRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(patient));

        PatientResponseDTO result = patientService.getPatientById(id, orgId);

        assertThat(result.getId()).isEqualTo(id.toString());
        assertThat(result.getName()).isEqualTo("Alice Smith");
    }

    @Test
    void getPatientById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findByIdAndOrganizationId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(id, "org-1"))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void createPatient_savesAndPublishesEvent() {
        String orgId = "org-1";
        UUID id = UUID.randomUUID();
        Patient saved = samplePatient(id, orgId);

        when(patientRepository.existsByEmailAndOrganizationId("alice@example.com", orgId)).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(saved);

        PatientResponseDTO result = patientService.createPatient(sampleRequest(), orgId);

        assertThat(result.getId()).isEqualTo(id.toString());
        verify(billingServiceGrpcClient).createBillingAccount(id.toString(), "Alice Smith", "alice@example.com", orgId);
        verify(kafkaProducer).sendEvent(saved, "PatientCreated", orgId, "");
    }

    @Test
    void createPatient_throws_onDuplicateEmail() {
        when(patientRepository.existsByEmailAndOrganizationId("alice@example.com", "org-1")).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(sampleRequest(), "org-1"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(patientRepository, never()).save(any());
    }

    @Test
    void updatePatient_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findByIdAndOrganizationId(id, "org-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.updatePatient(id, sampleRequest(), "org-1"))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void updatePatient_savesAndPublishesEvent() {
        UUID id = UUID.randomUUID();
        String orgId = "org-1";
        Patient existing = samplePatient(id, orgId);
        Patient saved = samplePatient(id, orgId);
        saved.setName("Updated Name");

        PatientRequestDTO req = new PatientRequestDTO();
        req.setName("Updated Name");
        req.setEmail("alice@example.com");
        req.setGender("FEMALE");
        req.setAddress("123 Main St");
        req.setDateOfBirth("1990-01-15");

        when(patientRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenReturn(saved);

        PatientResponseDTO result = patientService.updatePatient(id, req, orgId);

        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(kafkaProducer).sendEvent(saved, "PatientUpdated", orgId, "");
    }

    @Test
    void deletePatient_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findByIdAndOrganizationId(id, "org-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.deletePatient(id, "org-1"))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void deletePatient_deletesAndPublishesEvent() {
        UUID id = UUID.randomUUID();
        String orgId = "org-1";
        Patient patient = samplePatient(id, orgId);
        when(patientRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(patient));

        patientService.deletePatient(id, orgId);

        verify(billingServiceGrpcClient).deleteBillingAccount(id.toString(), orgId);
        verify(patientRepository).deleteById(id);
        verify(kafkaProducer).sendEvent(patient, "PatientDeleted", orgId, "");
    }
}
