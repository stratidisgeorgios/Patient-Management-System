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

    public PatientResponseDTO getPatientById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("Patient with ID " + id + " not found."));
        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with email " + patientRequestDTO.getEmail() + " already exists.");
        }
        Patient patient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
        billingServiceGrpcClient.createBillingAccount(patient.getId().toString(),patient.getName(),patient.getEmail());
        kafkaProducer.sendEvent(patient, "PatientCreated");
        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        Patient existingPatient = patientRepository.findById(id).orElseThrow(() -> new IdNotFoundException("Patient with ID " + id + " not found."));
        if (patientRequestDTO.getEmail() != null && !patientRequestDTO.getEmail().equals(existingPatient.getEmail()) && patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with email " + patientRequestDTO.getEmail() + " already exists.");
        }
        existingPatient.setName(patientRequestDTO.getName() != null ? patientRequestDTO.getName() : existingPatient.getName());
        existingPatient.setEmail(patientRequestDTO.getEmail() != null ? patientRequestDTO.getEmail() : existingPatient.getEmail());
        existingPatient.setGender(patientRequestDTO.getGender() != null ? com.patientsystem.patientservice.model.Gender.valueOf(patientRequestDTO.getGender()) : existingPatient.getGender());
        existingPatient.setAddress(patientRequestDTO.getAddress() != null ? patientRequestDTO.getAddress() : existingPatient.getAddress());
        existingPatient.setDateOfBirth(patientRequestDTO.getDateOfBirth() != null ? java.time.LocalDate.parse(patientRequestDTO.getDateOfBirth()) : existingPatient.getDateOfBirth());
        Patient saved = patientRepository.save(existingPatient);
        billingServiceGrpcClient.updateBillingAccount(saved.getId().toString(), saved.getName(), saved.getEmail());
        kafkaProducer.sendEvent(saved, "PatientUpdated");
        return PatientMapper.toDTO(saved);
    }

    public void deletePatient(UUID id){
        billingServiceGrpcClient.deleteBillingAccount(id.toString());
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new IdNotFoundException("Patient with ID " + id + " not found."));
        patientRepository.deleteById(id);
        kafkaProducer.sendEvent(patient, "PatientDeleted");
    }
}
