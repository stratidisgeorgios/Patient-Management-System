package com.patientsystem.patientservice.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.patientsystem.patientservice.dto.PatientRequestDTO;
import com.patientsystem.patientservice.dto.PatientResponseDTO;
import com.patientsystem.patientservice.exception.EmailAlreadyExistsException;
import com.patientsystem.patientservice.exception.IdNotFoundException;
import com.patientsystem.patientservice.grpc.BillingServiceGrpcClient;
import com.patientsystem.patientservice.kafka.KafkaProducer;
import com.patientsystem.patientservice.mapper.PatientMapper;
import com.patientsystem.patientservice.model.Patient;
import com.patientsystem.patientservice.repository.PatientRepository;

@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public PatientResponseDTO getPatientById(UUID id, String organizationId) {
        Patient patient = patientRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IdNotFoundException("Patient with ID " + id + " not found."));
        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO, String organizationId) {
        if (patientRepository.existsByEmailAndOrganizationId(patientRequestDTO.getEmail(), organizationId)) {
            throw new EmailAlreadyExistsException("A patient with email " + patientRequestDTO.getEmail() + " already exists.");
        }
        Patient patient = PatientMapper.toModel(patientRequestDTO);
        patient.setOrganizationId(organizationId);
        patient = patientRepository.save(patient);
        billingServiceGrpcClient.createBillingAccount(patient.getId().toString(), patient.getName(), patient.getEmail(), organizationId);
        kafkaProducer.sendEvent(patient, "PatientCreated", organizationId, "");
        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO, String organizationId) {
        Patient existingPatient = patientRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IdNotFoundException("Patient with ID " + id + " not found."));
        if (patientRequestDTO.getEmail() != null
                && !patientRequestDTO.getEmail().equals(existingPatient.getEmail())
                && patientRepository.existsByEmailAndOrganizationId(patientRequestDTO.getEmail(), organizationId)) {
            throw new EmailAlreadyExistsException("A patient with email " + patientRequestDTO.getEmail() + " already exists.");
        }
        existingPatient.setName(patientRequestDTO.getName() != null ? patientRequestDTO.getName() : existingPatient.getName());
        existingPatient.setEmail(patientRequestDTO.getEmail() != null ? patientRequestDTO.getEmail() : existingPatient.getEmail());
        existingPatient.setGender(patientRequestDTO.getGender() != null ? com.patientsystem.patientservice.model.Gender.valueOf(patientRequestDTO.getGender()) : existingPatient.getGender());
        existingPatient.setAddress(patientRequestDTO.getAddress() != null ? patientRequestDTO.getAddress() : existingPatient.getAddress());
        existingPatient.setDateOfBirth(patientRequestDTO.getDateOfBirth() != null ? java.time.LocalDate.parse(patientRequestDTO.getDateOfBirth()) : existingPatient.getDateOfBirth());
        Patient saved = patientRepository.save(existingPatient);
        billingServiceGrpcClient.updateBillingAccount(saved.getId().toString(), saved.getName(), saved.getEmail(), organizationId);
        kafkaProducer.sendEvent(saved, "PatientUpdated", organizationId, "");
        return PatientMapper.toDTO(saved);
    }

    public void deletePatient(UUID id, String organizationId) {
        Patient patient = patientRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IdNotFoundException("Patient with ID " + id + " not found."));
        billingServiceGrpcClient.deleteBillingAccount(id.toString(), organizationId);
        patientRepository.deleteById(id);
        kafkaProducer.sendEvent(patient, "PatientDeleted", organizationId, "");
    }
}
