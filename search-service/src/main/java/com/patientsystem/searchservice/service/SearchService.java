package com.patientsystem.searchservice.service;

import com.patientsystem.searchservice.documents.PatientDocument;
import com.patientsystem.searchservice.documents.TreatmentDocument;
import com.patientsystem.searchservice.opensearch.OpenSearchService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SearchService {
    private final OpenSearchService openSearchService;
    private final SseEmitterService sseEmitterService;

    public SearchService(OpenSearchService openSearchService, SseEmitterService sseEmitterService) {
        this.openSearchService = openSearchService;
        this.sseEmitterService = sseEmitterService;
    }

    public void indexPatient(PatientDocument patientDocument, String eventType) {
        try {
            if (eventType.equals("PatientCreated") || eventType.equals("PatientUpdated")) {
                openSearchService.indexPatient(patientDocument);
            } else if (eventType.equals("PatientDeleted")) {
                openSearchService.deletePatient(patientDocument.getId());
            }
            sseEmitterService.broadcast("patient");
        } catch (Exception e) {
            throw new RuntimeException("Failed to index patient: " + e.getMessage(), e);
        }
    }

    public void indexTreatment(TreatmentDocument treatmentDocument, String eventType) {
        try {
            if (eventType.equals("TreatmentCreated") || eventType.equals("TreatmentUpdated")) {
                openSearchService.indexTreatment(treatmentDocument);
            } else if (eventType.equals("TreatmentDeleted")) {
                openSearchService.deleteTreatment(treatmentDocument.getId());
            }
            sseEmitterService.broadcast("treatment");
        } catch (Exception e) {
            throw new RuntimeException("Failed to index treatment: " + e.getMessage(), e);
        }
    }

    public List<PatientDocument> searchPatients(String query) {
        try {
            return openSearchService.searchPatients(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search patients: " + e.getMessage(), e);
        }
    }

    public List<TreatmentDocument> searchTreatments(String query) {
        try {
            return openSearchService.searchTreatments(query);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search treatments: " + e.getMessage(), e);
        }
    }
}
