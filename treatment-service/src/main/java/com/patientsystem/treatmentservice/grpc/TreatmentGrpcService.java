package com.patientsystem.treatmentservice.grpc;

import java.util.UUID;
import com.patientsystem.treatmentservice.model.Treatment;
import com.patientsystem.treatmentservice.repository.TreatmentRepository;
import com.patientsystem.treatment.grpc.TreatmentServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import com.patientsystem.treatment.grpc.TreatmentRequest;
import com.patientsystem.treatment.grpc.TreatmentResponse;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class TreatmentGrpcService extends TreatmentServiceGrpc.TreatmentServiceImplBase {

    private final TreatmentRepository treatmentRepository;

    public TreatmentGrpcService(TreatmentRepository treatmentRepository) {
        this.treatmentRepository = treatmentRepository;
    }

    @Override
    public void getTreatment(TreatmentRequest treatmentRequest, StreamObserver<TreatmentResponse> responseObserver) {
        try {
            Treatment treatment = treatmentRepository.findById(UUID.fromString(treatmentRequest.getId()))
                    .orElseThrow(() -> new RuntimeException("Treatment not found: " + treatmentRequest.getId()));
            responseObserver.onNext(TreatmentResponse.newBuilder()
                    .setId(treatment.getId().toString())
                    .setName(treatment.getName())
                    .setCategory(treatment.getCategory() != null ? treatment.getCategory().getName() : "")
                    .setPrice(treatment.getPrice().toString())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
