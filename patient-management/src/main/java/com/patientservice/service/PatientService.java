package com.patientservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.patientservice.dto.PatientRequestDTO;
import com.patientservice.dto.PatientResponseDTO;
import com.patientservice.exception.EmailAlreadyExistsException;
import com.patientservice.exception.IdNotFoundException;
import com.patientservice.grpc.BillingServiceGrpcClient;
import com.patientservice.kafka.kafkaProducer;
import com.patientservice.mapper.PatientMapper;
import com.patientservice.model.Patient;
import com.patientservice.repository.PatientRepository;
@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final kafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, kafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public List<PatientResponseDTO> getAllPatients() {
        List<PatientResponseDTO> patients = patientRepository.findAll()
                .stream()
                .map(patient -> PatientMapper.toDTO(patient))
                .toList();  
        return patients;
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
        existingPatient.setAddress(patientRequestDTO.getAddress() != null ? patientRequestDTO.getAddress() : existingPatient.getAddress());
        existingPatient.setDateOfBirth(patientRequestDTO.getDateOfBirth() != null ? java.time.LocalDate.parse(patientRequestDTO.getDateOfBirth()) : existingPatient.getDateOfBirth());
        kafkaProducer.sendEvent(existingPatient, "PatientUpdated");
        return PatientMapper.toDTO(patientRepository.save(existingPatient));
    }

    public void deletePatient(UUID id){
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new IdNotFoundException("Patient with ID " + id + " not found."));
        patientRepository.deleteById(id);
        kafkaProducer.sendEvent(patient, "PatientDeleted");
    }
}
