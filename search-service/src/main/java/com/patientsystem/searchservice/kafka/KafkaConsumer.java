package com.patientsystem.searchservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.patientsystem.patient.kafka.PatientEvent;
import com.patientsystem.treatment.kafka.TreatmentEvent;
import com.patientsystem.searchservice.documents.PatientDocument;
import com.patientsystem.searchservice.documents.TreatmentDocument;
import com.patientsystem.searchservice.service.SearchService;
@Service
public class KafkaConsumer {
    Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    private final SearchService searchService;
    public KafkaConsumer(SearchService searchService) {
        this.searchService = searchService;
    }

    @KafkaListener(topics = "patient-events", groupId = "search-service")
    public void consumePatientEvent(byte[] event) {
        try {
            PatientEvent proto = PatientEvent.parseFrom(event);
            PatientDocument entity = new PatientDocument();
            entity.setPatientId(proto.getPatientId());
            entity.setName(proto.getName());
            entity.setEmail(proto.getEmail());
            entity.setDateOfBirth(proto.getDateOfBirth());
            entity.setGender(proto.getGender());
            searchService.indexPatient(entity, proto.getEventType());
            log.info("Consumed patient event: " + proto.getEventType() + " for patient: " + proto.getPatientId());
        } catch (Exception e) {
            log.error("Failed to consume patient event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "treatment-events", groupId = "search-service")
    public void consumeTreatmentEvent(byte[] event) {
        try {
            TreatmentEvent proto = TreatmentEvent.parseFrom(event);
            TreatmentDocument entity = new TreatmentDocument();
            entity.setTreatmentId(proto.getTreatmentId());
            entity.setName(proto.getName());
            entity.setCategory(proto.getCategory());
            entity.setPrice(proto.getPrice());
            searchService.indexTreatment(entity, proto.getEventType());
            log.info("Consumed treatment event: " + proto.getName() + " id: " + proto.getTreatmentId());
        } catch (Exception e) {
            log.error("Failed to consume treatment event: " + e.getMessage());
        }
    }
}


