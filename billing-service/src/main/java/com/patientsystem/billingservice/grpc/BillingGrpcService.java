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
    public void createBillingAccount(BillingRequest billingRequest, StreamObserver<BillingResponse> responseObserver) {
        BillingAccount billingAccount = billingService.createAccount(billingRequest.getPatientId(), billingRequest.getName(), billingRequest.getEmail());

        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId(billingAccount.getId())
                .setStatus("Billing account created for patient: " + billingRequest.getPatientId())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
