package com.patientsystem.patientservice.controller;

import java.util.UUID;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.patientsystem.patientservice.dto.*;
import com.patientsystem.patientservice.dto.validators.CreatePatientValidationGroup;
import com.patientsystem.patientservice.model.ImportJob;
import com.patientsystem.patientservice.service.ImportService;
import com.patientsystem.patientservice.service.PatientService;
import com.patientsystem.patientservice.service.S3Service;
import com.patientsystem.patientservice.repository.ImportJobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patient Management API", description = "API for managing patient records, including creation, retrieval, updating, and deletion of patient information.")
public class PatientController {
    private final PatientService patientService;
    private final ImportService importService;
    private final ImportJobRepository importJobRepository;
    private final S3Service s3Service;

    public PatientController(PatientService patientService, ImportService importService,
                             ImportJobRepository importJobRepository, S3Service s3Service) {
        this.patientService = patientService;
        this.importService = importService;
        this.importJobRepository = importJobRepository;
        this.s3Service = s3Service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable UUID id,
            @RequestHeader("X-Organization-Id") String organizationId) {
        return ResponseEntity.ok(patientService.getPatientById(id, organizationId));
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new patient")
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDTO patientRequestDTO,
            @RequestHeader("X-Organization-Id") String organizationId) {
        return ResponseEntity.ok(patientService.createPatient(patientRequestDTO, organizationId));
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update an existing patient")
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable UUID id,
            @Validated @RequestBody PatientRequestDTO patientRequestDTO,
            @RequestHeader("X-Organization-Id") String organizationId) {
        return ResponseEntity.ok(patientService.updatePatient(id, patientRequestDTO, organizationId));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a patient")
    public ResponseEntity<Void> deletePatient(
            @PathVariable UUID id,
            @RequestHeader("X-Organization-Id") String organizationId) {
        patientService.deletePatient(id, organizationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/import/presigned-url")
    @Operation(summary = "Get presigned S3 URL for direct browser upload")
    public ResponseEntity<PresignedUrlResponseDTO> getPresignedUrl(
            @RequestHeader("X-Organization-Id") String organizationId) {
        String s3Key = "imports/" + organizationId + "/" + UUID.randomUUID() + "/raw.csv";
        String url = s3Service.generatePresignedPutUrl(s3Key);
        return ResponseEntity.ok(new PresignedUrlResponseDTO(url, s3Key));
    }

    @PostMapping("/import/start")
    @Operation(summary = "Start async import with confirmed mapping")
    public ResponseEntity<ImportStartResponseDTO> startImport(
            @RequestBody ImportStartRequestDTO request,
            @RequestHeader("X-Organization-Id") String organizationId) {
        ImportJob job = new ImportJob();
        job.setOrganizationId(organizationId);
        job.setS3Key(request.getS3Key());
        job.setTotalRows(request.getTotalRows());
        job.setStatus("PENDING");
        job.setCreatedAt(LocalDateTime.now());
        ImportJob saved = importJobRepository.save(job);
        importService.sendToQueue(saved.getId().toString(), request.getS3Key(), request.getMapping(), organizationId);
        return ResponseEntity.accepted().body(new ImportStartResponseDTO(saved.getId().toString()));
    }

    @GetMapping("/import/status/{jobId}")
    @Operation(summary = "Get import job status")
    public ResponseEntity<ImportJob> getImportJobStatus(@PathVariable String jobId) {
        ImportJob job = importJobRepository.findById(UUID.fromString(jobId))
                .orElseThrow(() -> new RuntimeException("Import job not found"));
        return ResponseEntity.ok(job);
    }
}
