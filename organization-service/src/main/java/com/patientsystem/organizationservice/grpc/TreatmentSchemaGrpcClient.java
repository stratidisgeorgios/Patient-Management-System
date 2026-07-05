package com.patientsystem.organizationservice.grpc;

import com.patientsystem.organization.grpc.ProvisionSchemaRequest;
import com.patientsystem.organization.grpc.SchemaProvisionServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class TreatmentSchemaGrpcClient {

    @GrpcClient("treatment-service")
    private SchemaProvisionServiceGrpc.SchemaProvisionServiceBlockingStub stub;

    public void provisionSchema(String tenantId) {
        ProvisionSchemaRequest request = ProvisionSchemaRequest.newBuilder()
                .setTenantId(tenantId)
                .build();
        stub.provisionSchema(request);
    }
}
