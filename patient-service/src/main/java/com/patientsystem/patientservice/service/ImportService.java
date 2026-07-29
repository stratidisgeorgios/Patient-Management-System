package com.patientsystem.patientservice.service;

import com.patientsystem.patientservice.dto.ImportSqsMessage;
import com.patientsystem.patientservice.kafka.KafkaProducer;
import com.patientsystem.patientservice.model.*;
import com.patientsystem.patientservice.repository.*;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.apache.commons.csv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ImportService {
    private static final Logger log = LoggerFactory.getLogger(ImportService.class);
    private static final int CHUNK_SIZE = 5000;

    private final SqsTemplate sqsTemplate;
    private final S3Service s3Service;
    private final PatientRepository patientRepository;
    private final ImportJobRepository importJobRepository;
    private final KafkaProducer kafkaProducer;

    @Value("${aws.sqs.import-queue-url}")
    private String queueUrl;

    public ImportService(SqsTemplate sqsTemplate, S3Service s3Service, PatientRepository patientRepository,
                         ImportJobRepository importJobRepository, KafkaProducer kafkaProducer) {
        this.sqsTemplate = sqsTemplate;
        this.s3Service = s3Service;
        this.patientRepository = patientRepository;
        this.importJobRepository = importJobRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public void sendToQueue(String jobId, String s3Key, Map<String, String> mapping, String organizationId) {
        sqsTemplate.send(queueUrl, new ImportSqsMessage(jobId, s3Key, mapping, organizationId));
    }

    @SqsListener("${aws.sqs.import-queue-url}")
    public void processMessage(ImportSqsMessage message) {
        processJob(message);
    }

    private void processJob(ImportSqsMessage message) {
        ImportJob job = importJobRepository.findById(UUID.fromString(message.getJobId())).orElseThrow();
        job.setStatus("PROCESSING");
        importJobRepository.save(job);

        List<Map<String, Object>> errors = new ArrayList<>();
        int imported = 0;

        try (InputStream stream = openStream(message.getS3Key());
             Reader reader = new InputStreamReader(stream)) {

            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            List<Patient> chunk = new ArrayList<>();

            for (CSVRecord record : records) {
                try {
                    chunk.add(mapRow(record, message.getMapping()));
                    if (chunk.size() == CHUNK_SIZE) {
                        imported += saveChunk(chunk, message.getOrganizationId());
                        chunk.clear();
                        job.setImportedRows(imported);
                        importJobRepository.save(job);
                    }
                } catch (Exception e) {
                    errors.add(Map.of("row", record.getRecordNumber(), "reason", e.getMessage()));
                }
            }

            if (!chunk.isEmpty()) imported += saveChunk(chunk, message.getOrganizationId());

        } catch (Exception e) {
            log.error("Import failed for job {}: {}", message.getJobId(), e.getMessage());
            job.setStatus("FAILED");
            importJobRepository.save(job);
            return;
        }

        job.setImportedRows(imported);
        job.setFailedRows(errors.size());
        job.setErrors(errors);
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(job);
        kafkaProducer.sendImportCompleted(message.getJobId(), message.getOrganizationId());
    }

    private int saveChunk(List<Patient> chunk, String organizationId) {
        chunk.forEach(p -> p.setOrganizationId(organizationId));
        return patientRepository.saveAll(chunk).size();
    }

    private Patient mapRow(CSVRecord record, Map<String, String> mapping) {
        Patient patient = new Patient();
        Map<String, Object> customFields = new HashMap<>();

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String value = record.get(entry.getKey());
            if (value == null || value.isBlank()) continue;
            switch (entry.getValue()) {
                case "name" -> patient.setName(value);
                case "email" -> patient.setEmail(value);
                case "gender" -> patient.setGender(Gender.valueOf(value.toUpperCase()));
                case "address" -> patient.setAddress(value);
                case "dateOfBirth" -> patient.setDateOfBirth(LocalDate.parse(value));
                case "registeredDate" -> patient.setRegisteredDate(LocalDate.parse(value));
                default -> customFields.put(entry.getValue(), value);
            }
        }

        if (patient.getRegisteredDate() == null) patient.setRegisteredDate(LocalDate.now());
        patient.setCustomFields(customFields);
        return patient;
    }

    private InputStream openStream(String s3Key) throws IOException {
        InputStream raw = s3Service.download(s3Key);
        try {
            return s3Key.endsWith(".gz") ? new GZIPInputStream(raw) : raw;
        } catch (IOException e) {
            raw.close();
            throw e;
        }
    }
}
