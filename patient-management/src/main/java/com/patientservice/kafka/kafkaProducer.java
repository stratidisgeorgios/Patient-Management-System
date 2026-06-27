package com.patientservice.kafka;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.patientservice.model.Patient;

import patient.events.PatientEvent;

@Service
public class kafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(kafkaProducer.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public kafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient, String eventType) {
        PatientEvent patientEvent = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setGender(patient.getGender().toString())
                .setDateOfBirth(patient.getDateOfBirth().toString())
                .setEventType(eventType)
                .setTimestamp(LocalDateTime.now().toString())
                .build();
        try {
            kafkaTemplate.send("patient-events", patientEvent.toByteArray());
        } catch (Exception e) {
            log.error("Failed to send patient event: " + e.getMessage());
        }
    }
}
