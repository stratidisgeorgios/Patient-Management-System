package com.patientsystem.treatmentservice.kafka;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.patientsystem.treatmentservice.model.Treatment;

import com.patientsystem.treatment.kafka.TreatmentEvent;
@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTreatmentEvent(Treatment treatment, String eventType) {
        TreatmentEvent treatmentEvent = TreatmentEvent.newBuilder()
            .setTreatmentId(treatment.getId())
            .setName(treatment.getName())
            .setCategory(treatment.getCategory().getName())
            .setPrice(treatment.getPrice().toString())
            .setEventType(eventType)
            .build();
        try {
            kafkaTemplate.send("treatment-events", treatmentEvent.toByteArray());
        } catch (Exception e) {
            log.error("Failed to send treatment event: " + e.getMessage());
        }
    }
    
}
