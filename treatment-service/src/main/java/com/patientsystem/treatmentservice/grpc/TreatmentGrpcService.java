package com.patientsystem.treatmentservice.grpc;

import com.patientsystem.treatmentservice.service.TreatmentService;
import com.patientsystem.treatment.grpc.TreatmentServiceGrpc;
import io.grpc.stub.StreamObserver;
import com.patientsystem.treatmentservice.dto.TreatmentResponseDTO;
import com.patientsystem.treatment.grpc.TreatmentRequest;
import com.patientsystem.treatment.grpc.TreatmentResponse;
import net.devh.boot.grpc.server.service.GrpcService;
@GrpcService
public class TreatmentGrpcService extends TreatmentServiceGrpc.TreatmentServiceImplBase {
    
    private final TreatmentService treatmentService;
    public TreatmentGrpcService(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }
    @Override
    public void getTreatment(TreatmentRequest treatmentRequest, StreamObserver<TreatmentResponse> responseObserver) {
        
        TreatmentResponseDTO found = treatmentService.getTreatmentById(treatmentRequest.getId());

        TreatmentResponse response = TreatmentResponse.newBuilder()
                .setId(found.getId())
                .setName(found.getName())
                .setCategory(found.getCategory().getName())
                .setPrice(found.getPrice().toString())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}