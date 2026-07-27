package com.patientsystem.billingservice.grpc;

import com.patientsystem.billingservice.service.BillingService;
import com.patientsystem.billingservice.model.BillingAccount;
import com.patientsystem.billing.grpc.BillingServiceGrpc;
import com.patientsystem.billing.grpc.BillingRequest;
import com.patientsystem.billing.grpc.BillingResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {

    private final BillingService billingService;

    public BillingGrpcService(BillingService billingService) {
        this.billingService = billingService;
    }

    @Override
    public void createBillingAccount(BillingRequest request, StreamObserver<BillingResponse> responseObserver) {
        BillingAccount account = billingService.createAccount(
                request.getPatientId(), request.getName(), request.getEmail(), request.getOrganizationId());
        responseObserver.onNext(BillingResponse.newBuilder()
                .setAccountId(account.getId().toString())
                .setStatus("Billing account created for patient: " + request.getPatientId())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteBillingAccount(BillingRequest request, StreamObserver<BillingResponse> responseObserver) {
        billingService.deleteAccount(request.getPatientId(), request.getOrganizationId());
        responseObserver.onNext(BillingResponse.newBuilder()
                .setStatus("Billing account deleted for patient: " + request.getPatientId())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateBillingAccount(BillingRequest request, StreamObserver<BillingResponse> responseObserver) {
        BillingAccount account = billingService.updateAccount(
                request.getPatientId(), request.getName(), request.getEmail(), request.getOrganizationId());
        responseObserver.onNext(BillingResponse.newBuilder()
                .setAccountId(account.getId().toString())
                .setStatus("Billing account updated for patient: " + request.getPatientId())
                .build());
        responseObserver.onCompleted();
    }
}
