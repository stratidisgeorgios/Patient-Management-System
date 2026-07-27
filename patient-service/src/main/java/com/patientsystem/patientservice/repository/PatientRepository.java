package com.patientsystem.patientservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patientsystem.patientservice.model.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByEmailAndOrganizationId(String email, String organizationId);
    Page<Patient> findAllByOrganizationId(String organizationId, Pageable pageable);
    Optional<Patient> findByIdAndOrganizationId(UUID id, String organizationId);
}
