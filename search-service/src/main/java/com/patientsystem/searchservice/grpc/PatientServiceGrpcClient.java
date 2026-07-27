package com.patientsystem.searchservice.grpc;

import com.patientsystem.patient.grpc.OrganizationPageRequest;
import com.patientsystem.patient.grpc.PatientPageResponse;
import com.patientsystem.patient.grpc.PatientServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PatientServiceGrpcClient {

    private final PatientServiceGrpc.PatientServiceBlockingStub stub;

    public PatientServiceGrpcClient(
            @Value("${patient.service.address:localhost}") String address,
            @Value("${patient.service.grpc.port:9003}") int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port)
                .usePlaintext()
                .build();
        stub = PatientServiceGrpc.newBlockingStub(channel);
    }

    public PatientPageResponse getPatientsByOrganization(String organizationId, int page, int size) {
        return stub.getPatientsByOrganization(
                OrganizationPageRequest.newBuilder()
                        .setOrganizationId(organizationId)
                        .setPage(page)
                        .setSize(size)
                        .build());
    }
}
