package com.patientservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patientservice.model.Patient;



@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    @Override
    boolean existsById(UUID id);
    boolean existsByEmail(String email);
}
