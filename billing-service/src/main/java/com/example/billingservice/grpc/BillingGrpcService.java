package com.example.billingservice.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.billingservice.service.BillingService;

import billing.BillingServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
@GrpcService
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {
    
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);
    private final BillingService billingService;
    public BillingGrpcService(BillingService billingService) {
        this.billingService = billingService;
    }
    @Override
    public void createBillingAccount(billing.BillingRequest billingRequest, StreamObserver<billing.BillingResponse> responseObserver) {
        log.info("Received billing request for patientId: {}, name: {}, email: {}",
            billingRequest.getPatientId(),
            billingRequest.getName(),
            billingRequest.getEmail());
        billingService.createAccount(billingRequest.getPatientId(), billingRequest.getName(), billingRequest.getEmail());
        
        billing.BillingResponse response = billing.BillingResponse.newBuilder()
                .setAccountId("ACC123456") // This would be generated dynamically in a real application
                .setStatus("Billing account created successfully for user: " + billingRequest.toString())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
