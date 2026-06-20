package com.example.analyticsservice.kafka;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import patient.events.PatientEvent;
@Service
public class KafkaConsumer {
    Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
  
        @KafkaListener(topics = "patient-events", groupId = "analytics-service")
        public void consumeEvent(byte[] event){
            try {
                PatientEvent patientEvent = PatientEvent.parseFrom(event);
                //perform any business related to analytics here
                log.info("Consumed patient event: " + patientEvent.toString());
            } catch (Exception e) {
                // Handle exception
                log.error("Failed to consume patient event: " + e.getMessage());
            }
        }
}
