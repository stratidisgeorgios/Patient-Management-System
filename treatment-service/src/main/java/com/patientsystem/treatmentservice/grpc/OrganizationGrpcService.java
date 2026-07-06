package com.patientsystem.treatmentservice.grpc;

import com.patientsystem.organization.grpc.ProvisionSchemaRequest;
import com.patientsystem.organization.grpc.ProvisionSchemaResponse;
import com.patientsystem.organization.grpc.SchemaProvisionServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@GrpcService
public class OrganizationGrpcService extends SchemaProvisionServiceGrpc.SchemaProvisionServiceImplBase {

    private final DataSource dataSource;

    public OrganizationGrpcService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void provisionSchema(ProvisionSchemaRequest request, StreamObserver<ProvisionSchemaResponse> responseObserver) {
        try {
            String tenantId = request.getTenantId();
            dataSource.getConnection().createStatement().execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(tenantId)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            responseObserver.onNext(ProvisionSchemaResponse.newBuilder().setStatus("OK").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
