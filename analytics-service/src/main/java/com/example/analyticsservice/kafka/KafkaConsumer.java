package com.example.analyticsservice.kafka;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.example.analyticsservice.repository.ChargeEventRepository;
import com.example.analyticsservice.repository.PatientEventRepository;
import patient.events.PatientEvent;
import billing.ChargeEvent;

@Service
public class KafkaConsumer {
    Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    private final PatientEventRepository patientEventRepository;
    private final ChargeEventRepository chargeEventRepository;

    public KafkaConsumer(PatientEventRepository patientEventRepository, ChargeEventRepository chargeEventRepository) {
        this.patientEventRepository = patientEventRepository;
        this.chargeEventRepository = chargeEventRepository;
    }

    @KafkaListener(topics = "patient-events", groupId = "analytics-service")
    public void consumePatientEvent(byte[] event) {
        try {
            PatientEvent proto = PatientEvent.parseFrom(event);
            com.example.analyticsservice.model.PatientEvent entity = new com.example.analyticsservice.model.PatientEvent();
            entity.setPatientId(proto.getPatientId());
            entity.setEventType(proto.getEventType());
            entity.setDateOfBirth(proto.getDateOfBirth());
            entity.setTimestamp(LocalDateTime.now());
            patientEventRepository.save(entity);
            log.info("Consumed patient event: " + proto.getEventType() + " for patient: " + proto.getPatientId());
        } catch (Exception e) {
            log.error("Failed to consume patient event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "billing-events", groupId = "analytics-service")
    public void consumeChargeEvent(byte[] event) {
        try {
            ChargeEvent proto = ChargeEvent.parseFrom(event);
            com.example.analyticsservice.model.ChargeEvent entity = new com.example.analyticsservice.model.ChargeEvent();
            entity.setPatientId(proto.getPatientId());
            entity.setTreatmentName(proto.getTreatmentName());
            entity.setCategory(proto.getCategory());
            entity.setPrice(proto.getPrice());
            entity.setTimestamp(LocalDateTime.now());
            chargeEventRepository.save(entity);
            log.info("Consumed charge event: " + proto.getTreatmentName() + " for patient: " + proto.getPatientId());
        } catch (Exception e) {
            log.error("Failed to consume charge event: " + e.getMessage());
        }
    }
}
