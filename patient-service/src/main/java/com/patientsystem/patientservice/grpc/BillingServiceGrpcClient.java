package com.patientsystem.patientservice.grpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.patientsystem.billing.grpc.BillingRequest;
import com.patientsystem.billing.grpc.BillingResponse;
import com.patientsystem.billing.grpc.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component
public class BillingServiceGrpcClient {

    private final BillingServiceGrpc.BillingServiceBlockingStub billingServiceBlockingStub;

    public BillingServiceGrpcClient(@Value("${billing.service.address:localhost}") String billingServiceAddress,
                                    @Value("${billing.service.grpc.port:9001}") int billingServicePort) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(billingServiceAddress, billingServicePort)
                .usePlaintext()
                .build();
        billingServiceBlockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(String patientId, String name, String email, String organizationId) {
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId).setName(name).setEmail(email).setOrganizationId(organizationId).build();
        return billingServiceBlockingStub.createBillingAccount(request);
    }

    public BillingResponse deleteBillingAccount(String patientId, String organizationId) {
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId).setOrganizationId(organizationId).build();
        return billingServiceBlockingStub.deleteBillingAccount(request);
    }

    public BillingResponse updateBillingAccount(String patientId, String name, String email, String organizationId) {
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId).setName(name).setEmail(email).setOrganizationId(organizationId).build();
        return billingServiceBlockingStub.updateBillingAccount(request);
    }
}
