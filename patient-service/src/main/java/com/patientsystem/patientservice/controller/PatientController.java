package com.patientsystem.patientservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.patientsystem.patientservice.dto.PatientRequestDTO;
import com.patientsystem.patientservice.dto.PatientResponseDTO;
import com.patientsystem.patientservice.dto.validators.CreatePatientValidationGroup;
import com.patientsystem.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import io.swagger.v3.oas.annotations.Operation;
import com.patientsystem.patientservice.dto.*;
import com.patientsystem.patientservice.mapper.ImportColumnMapper;
import com.patientsystem.patientservice.model.ImportJob;
import com.patientsystem.patientservice.service.ImportService;
import com.patientsystem.patientservice.service.S3Service;
import com.patientsystem.patientservice.repository.ImportJobRepository;
import org.apache.commons.csv.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patient Management API", description = "API for managing patient records, including creation, retrieval, updating, and deletion of patient information.")  
public class PatientController {
    private final PatientService patientService;
    private final ImportService importService;
    private final ImportColumnMapper importColumnMapper;
    private final ImportJobRepository importJobRepository;
    private final S3Service s3Service;

    public PatientController(PatientService patientService, ImportService importService, ImportColumnMapper importColumnMapper, ImportJobRepository importJobRepository, S3Service s3Service) {
        this.patientService = patientService;
        this.importService = importService;
        this.importColumnMapper = importColumnMapper;
        this.importJobRepository = importJobRepository;
        this.s3Service = s3Service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID", description = "Retrieve a specific patient's information by their unique ID.")
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable UUID id) {
        PatientResponseDTO patient = patientService.getPatientById(id);
        return ResponseEntity.ok(patient);
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new patient", description = "Create a new patient record in the system. The request body must contain valid patient information.")
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDTO patientRequestDTO,
            @RequestHeader("X-Organization-Id") String organizationId) {
        PatientResponseDTO newPatient = patientService.createPatient(patientRequestDTO, organizationId);
        return ResponseEntity.ok(newPatient);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update an existing patient", description = "Update the information of an existing patient identified by their unique ID. The request body must contain valid updated patient information.")
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable UUID id,
            @Validated @RequestBody PatientRequestDTO patientRequestDTO,
            @RequestHeader("X-Organization-Id") String organizationId) {
        PatientResponseDTO updatedPatient = patientService.updatePatient(id, patientRequestDTO, organizationId);
        return ResponseEntity.ok(updatedPatient);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a patient", description = "Delete an existing patient record from the system identified by their unique ID.")
    public ResponseEntity<Void> deletePatient(
            @PathVariable UUID id,
            @RequestHeader("X-Organization-Id") String organizationId) {
        patientService.deletePatient(id, organizationId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping(value = "/import/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload CSV and get column mapping preview")
    public ResponseEntity<ImportUploadResponseDTO> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Organization-Id") String organizationId) throws Exception {

        byte[] fileBytes = file.getBytes();
        List<String> headers;
        int totalRows = 0;

        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(fileBytes))) {
            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            headers = new ArrayList<>(parser.getHeaderNames());
            for (CSVRecord ignored : parser) totalRows++;
        }

        String s3Key = "imports/" + organizationId + "/" + UUID.randomUUID() + "/raw.csv";
        s3Service.upload(s3Key, fileBytes);

        return ResponseEntity.ok(new ImportUploadResponseDTO(s3Key, importColumnMapper.mapHeaders(headers), totalRows));
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
    @Operation(summary = "Get import job status", description = "Retrieve the status and details of an import job by its unique ID.")
    public ResponseEntity<ImportJob> getImportJobStatus(@PathVariable String jobId) {
        ImportJob job = importJobRepository.findById(UUID.fromString(jobId)).orElseThrow(() -> new RuntimeException("Import job not found")); 
        return ResponseEntity.ok(job);
    }

}
