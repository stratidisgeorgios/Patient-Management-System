package com.patientsystem.patientservice.repository;

import com.patientsystem.patientservice.model.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {}
