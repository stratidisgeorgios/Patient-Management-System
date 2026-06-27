package com.example.billingservice.kafka;

import billing.ChargeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendChargeEvent(String patientId, String treatmentName, String category, String price, String timestamp) {
        ChargeEvent chargeEvent = ChargeEvent.newBuilder()
                .setPatientId(patientId)
                .setTreatmentName(treatmentName)
                .setCategory(category)
                .setPrice(price)
                .setTimestamp(timestamp)
                .build();
        try {
            kafkaTemplate.send("billing-events", chargeEvent.toByteArray());
        } catch (Exception e) {
            log.error("Failed to send charge event: " + e.getMessage());
        }
    }
}
