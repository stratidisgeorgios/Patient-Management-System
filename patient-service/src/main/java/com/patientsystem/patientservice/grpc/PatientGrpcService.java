package com.patientsystem.patientservice.grpc;

import com.patientsystem.patient.grpc.OrganizationPageRequest;
import com.patientsystem.patient.grpc.PatientPageResponse;
import com.patientsystem.patient.grpc.PatientRequest;
import com.patientsystem.patient.grpc.PatientResponse;
import com.patientsystem.patient.grpc.PatientServiceGrpc;
import com.patientsystem.patientservice.model.Patient;
import com.patientsystem.patientservice.repository.PatientRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

@GrpcService
public class PatientGrpcService extends PatientServiceGrpc.PatientServiceImplBase {

    private final PatientRepository patientRepository;

    public PatientGrpcService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public void getPatient(PatientRequest request, StreamObserver<PatientResponse> responseObserver) {
        try {
            UUID id = UUID.fromString(request.getPatientId());
            Patient patient = patientRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Patient not found: " + id));

            responseObserver.onNext(PatientResponse.newBuilder()
                    .setId(patient.getId().toString())
                    .setName(patient.getName() != null ? patient.getName() : "")
                    .setEmail(patient.getEmail() != null ? patient.getEmail() : "")
                    .setGender(patient.getGender() != null ? patient.getGender().name() : "")
                    .setDateOfBirth(patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : "")
                    .setAddress(patient.getAddress() != null ? patient.getAddress() : "")
                    .setRegisteredDate(patient.getRegisteredDate() != null ? patient.getRegisteredDate().toString() : "")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getPatientsByOrganization(OrganizationPageRequest request, StreamObserver<PatientPageResponse> responseObserver) {
        try {
            Page<Patient> pageResult = patientRepository.findAllByOrganizationId(
                    request.getOrganizationId(),
                    PageRequest.of(request.getPage(), request.getSize()));
            PatientPageResponse.Builder response = PatientPageResponse.newBuilder()
                    .setHasNext(pageResult.hasNext());
            pageResult.getContent().forEach(patient -> response.addPatients(
                PatientResponse.newBuilder()
                    .setId(patient.getId().toString())
                    .setName(patient.getName() != null ? patient.getName() : "")
                    .setEmail(patient.getEmail() != null ? patient.getEmail() : "")
                    .setGender(patient.getGender() != null ? patient.getGender().name() : "")
                    .setDateOfBirth(patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : "")
                    .setAddress(patient.getAddress() != null ? patient.getAddress() : "")
                    .setRegisteredDate(patient.getRegisteredDate() != null ? patient.getRegisteredDate().toString() : "")
                    .build()
            ));
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
