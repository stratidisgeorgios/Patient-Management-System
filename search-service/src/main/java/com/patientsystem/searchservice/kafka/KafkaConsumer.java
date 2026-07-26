package com.patientsystem.searchservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.patientsystem.patient.kafka.PatientEvent;
import com.patientsystem.import_.kafka.ImportEvent;
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
            entity.setId(proto.getPatientId());
            entity.setName(proto.getName());
            entity.setEmail(proto.getEmail());
            entity.setDateOfBirth(proto.getDateOfBirth());
            entity.setGender(proto.getGender());
            entity.setOrganizationId(proto.getOrganizationId());
            searchService.indexPatient(entity, proto.getEventType(), proto.getJobId());
            log.info("Consumed patient event: " + proto.getEventType() + " for patient: " + proto.getPatientId());
        } catch (Exception e) {
            log.error("Failed to consume patient event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "import-events", groupId = "search-service")
    public void consumeImportEvent(byte[] event) {
        try {
            ImportEvent proto = ImportEvent.parseFrom(event);
            searchService.broadcastImportComplete();
            log.info("Import completed for job: " + proto.getJobId());
        } catch (Exception e) {
            log.error("Failed to consume import event: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "treatment-events", groupId = "search-service")
    public void consumeTreatmentEvent(byte[] event) {
        try {
            TreatmentEvent proto = TreatmentEvent.parseFrom(event);
            TreatmentDocument entity = new TreatmentDocument();
            entity.setId(proto.getTreatmentId());
            entity.setName(proto.getName());
            entity.setCategory(proto.getCategory());
            entity.setPrice(proto.getPrice());
            entity.setOrganizationId(proto.getOrganizationId());
            searchService.indexTreatment(entity, proto.getEventType());
            log.info("Consumed treatment event: " + proto.getName() + " id: " + proto.getTreatmentId());
        } catch (Exception e) {
            log.error("Failed to consume treatment event: " + e.getMessage());
        }
    }
}


