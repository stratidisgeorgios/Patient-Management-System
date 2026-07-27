package com.patientsystem.billingservice.kafka;

import com.patientsystem.import_.kafka.ImportEvent;
import com.patientsystem.patient.grpc.PatientPageResponse;
import com.patientsystem.billingservice.grpc.PatientServiceGrpcClient;
import com.patientsystem.billingservice.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    private final BillingService billingService;
    private final PatientServiceGrpcClient patientServiceGrpcClient;

    public KafkaConsumer(BillingService billingService, PatientServiceGrpcClient patientServiceGrpcClient) {
        this.billingService = billingService;
        this.patientServiceGrpcClient = patientServiceGrpcClient;
    }

    @KafkaListener(topics = "import-events", groupId = "billing-service")
    public void consumeImportEvent(byte[] event) {
        try {
            ImportEvent proto = ImportEvent.parseFrom(event);
            log.info("Import completed for job: {}, creating billing accounts for org: {}", proto.getJobId(), proto.getOrganizationId());
            int page = 0;
            int count = 0;
            boolean hasNext = true;
            while (hasNext) {
                PatientPageResponse response = patientServiceGrpcClient.getPatientsByOrganization(proto.getOrganizationId(), page, 1000);
                hasNext = response.getHasNext();
                page++;
                for (var p : response.getPatientsList()) {
                    billingService.createAccount(p.getId(), p.getName(), p.getEmail(), proto.getOrganizationId());
                    count++;
                }
            }
            log.info("Created {} billing accounts for job: {}", count, proto.getJobId());
        } catch (Exception e) {
            log.error("Failed to process import event for billing: " + e.getMessage());
        }
    }
}
