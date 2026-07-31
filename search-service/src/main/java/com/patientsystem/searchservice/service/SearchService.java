package com.patientsystem.searchservice.service;

import com.patientsystem.patient.grpc.PatientPageResponse;
import com.patientsystem.patient.grpc.PatientResponse;
import com.patientsystem.searchservice.documents.PatientDocument;
import com.patientsystem.searchservice.documents.TreatmentDocument;
import com.patientsystem.searchservice.grpc.PatientServiceGrpcClient;
import com.patientsystem.searchservice.opensearch.OpenSearchService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SearchService {
    private final OpenSearchService openSearchService;
    private final SseEmitterService sseEmitterService;
    private final PatientServiceGrpcClient patientServiceGrpcClient;

    public SearchService(OpenSearchService openSearchService, SseEmitterService sseEmitterService,
                         PatientServiceGrpcClient patientServiceGrpcClient) {
        this.openSearchService = openSearchService;
        this.sseEmitterService = sseEmitterService;
        this.patientServiceGrpcClient = patientServiceGrpcClient;
    }

    public void indexPatient(PatientDocument patientDocument, String eventType, String jobId) {
        try {
            if (eventType.equals("PatientCreated") || eventType.equals("PatientUpdated")) {
                openSearchService.indexPatient(patientDocument);
            } else if (eventType.equals("PatientDeleted")) {
                openSearchService.deletePatient(patientDocument.getId(), patientDocument.getOrganizationId());
            }
            if (jobId == null || jobId.isEmpty()) {
                sseEmitterService.broadcast("patient");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to index patient: " + e.getMessage(), e);
        }
    }

    public void bulkIndexAndBroadcast(String organizationId) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try {
            int page = 0;
            boolean hasNext = true;
            while (hasNext) {
                PatientPageResponse response = patientServiceGrpcClient.getPatientsByOrganization(organizationId, page, 1000);
                hasNext = response.getHasNext();
                page++;
                List<PatientDocument> batch = new ArrayList<>();
                for (PatientResponse p : response.getPatientsList()) {
                    PatientDocument doc = new PatientDocument();
                    doc.setId(p.getId());
                    doc.setName(p.getName());
                    doc.setEmail(p.getEmail());
                    doc.setGender(p.getGender());
                    doc.setDateOfBirth(p.getDateOfBirth());
                    doc.setOrganizationId(organizationId);
                    batch.add(doc);
                }
                if (!batch.isEmpty()) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        try { openSearchService.bulkIndexPatients(batch, organizationId); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    }, executor));
                }
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            sseEmitterService.broadcast("patient");
        } catch (Exception e) {
            throw new RuntimeException("Failed to bulk index patients: " + e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }

    public void indexTreatment(TreatmentDocument treatmentDocument, String eventType) {
        try {
            if (eventType.equals("TreatmentCreated") || eventType.equals("TreatmentUpdated")) {
                openSearchService.indexTreatment(treatmentDocument);
            } else if (eventType.equals("TreatmentDeleted")) {
                openSearchService.deleteTreatment(treatmentDocument.getId(), treatmentDocument.getOrganizationId());
            }
            sseEmitterService.broadcast("treatment");
        } catch (Exception e) {
            throw new RuntimeException("Failed to index treatment: " + e.getMessage(), e);
        }
    }

    public List<PatientDocument> searchPatients(String query, String organizationId) {
        try {
            return openSearchService.searchPatients(query, organizationId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search patients: " + e.getMessage(), e);
        }
    }

    public List<TreatmentDocument> searchTreatments(String query, String organizationId) {
        try {
            return openSearchService.searchTreatments(query, organizationId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search treatments: " + e.getMessage(), e);
        }
    }
}
