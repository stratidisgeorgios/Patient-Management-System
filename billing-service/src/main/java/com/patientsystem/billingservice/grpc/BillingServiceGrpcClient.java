package com.patientsystem.billingservice.grpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.patientsystem.treatment.grpc.TreatmentRequest;
import com.patientsystem.treatment.grpc.TreatmentResponse;
import com.patientsystem.treatment.grpc.TreatmentServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component
public class BillingServiceGrpcClient {
  
    private final TreatmentServiceGrpc.TreatmentServiceBlockingStub treatmentServiceBlockingStub;

    public BillingServiceGrpcClient(@Value("${treatment.service.address:localhost}")String treatmentServiceAddress,
                                    @Value("${treatment.service.grpc.port:9001}") int treatmentServicePort)
    {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(treatmentServiceAddress, treatmentServicePort)
                .usePlaintext()
                .build();
        treatmentServiceBlockingStub = TreatmentServiceGrpc.newBlockingStub(channel);
    }

    public TreatmentResponse getTreatment(String id){
        TreatmentRequest request = TreatmentRequest.newBuilder().setId(id).build();
        TreatmentResponse response = treatmentServiceBlockingStub.getTreatment(request);
        return response;
    }
}
