package com.patientsystem.billingservice.grpc;

import com.patientsystem.patient.grpc.PatientRequest;
import com.patientsystem.patient.grpc.PatientResponse;
import com.patientsystem.patient.grpc.PatientServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PatientServiceGrpcClient {

    private final PatientServiceGrpc.PatientServiceBlockingStub patientServiceBlockingStub;

    public PatientServiceGrpcClient(
            @Value("${patient.service.address:localhost}") String patientServiceAddress,
            @Value("${patient.service.grpc.port:9003}") int patientServicePort) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(patientServiceAddress, patientServicePort)
                .usePlaintext()
                .build();
        patientServiceBlockingStub = PatientServiceGrpc.newBlockingStub(channel);
    }

    public PatientResponse getPatient(String patientId) {
        PatientRequest request = PatientRequest.newBuilder().setPatientId(patientId).build();
        return patientServiceBlockingStub.getPatient(request);
    }
}
