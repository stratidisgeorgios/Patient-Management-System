package com.patientsystem.patientservice.kafka;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.patientsystem.patientservice.model.Patient;

import com.patientsystem.patient.kafka.PatientEvent;
import com.patientsystem.import_.kafka.ImportEvent;


@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient, String eventType, String organizationId, String jobId) {
        PatientEvent patientEvent = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setGender(patient.getGender().toString())
                .setDateOfBirth(patient.getDateOfBirth().toString())
                .setEventType(eventType)
                .setTimestamp(LocalDateTime.now().toString())
                .setOrganizationId(organizationId)
                .setJobId(jobId != null ? jobId : "")
                .build();
        try {
            kafkaTemplate.send("patient-events", patientEvent.toByteArray());
        } catch (Exception e) {
            log.error("Failed to send patient event: " + e.getMessage());
        }
    }

    public void sendImportCompleted(String jobId, String organizationId) {
        ImportEvent event = ImportEvent.newBuilder()
                .setJobId(jobId)
                .setOrganizationId(organizationId)
                .build();
        try {
            kafkaTemplate.send("import-events", event.toByteArray());
        } catch (Exception e) {
            log.error("Failed to send ImportCompleted event: " + e.getMessage());
        }
    }
}
