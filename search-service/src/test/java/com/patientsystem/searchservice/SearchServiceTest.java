package com.patientsystem.searchservice;

import com.patientsystem.searchservice.documents.PatientDocument;
import com.patientsystem.searchservice.documents.TreatmentDocument;
import com.patientsystem.searchservice.grpc.PatientServiceGrpcClient;
import com.patientsystem.searchservice.opensearch.OpenSearchService;
import com.patientsystem.searchservice.service.SearchService;
import com.patientsystem.searchservice.service.SseEmitterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    OpenSearchService openSearchService;
    @Mock
    SseEmitterService sseEmitterService;
    @Mock
    PatientServiceGrpcClient patientServiceGrpcClient;

    @InjectMocks
    SearchService searchService;

    private PatientDocument samplePatientDoc() {
        PatientDocument doc = new PatientDocument();
        doc.setId("patient-1");
        doc.setName("Alice");
        doc.setEmail("alice@example.com");
        doc.setOrganizationId("org-1");
        return doc;
    }

    private TreatmentDocument sampleTreatmentDoc() {
        TreatmentDocument doc = new TreatmentDocument();
        doc.setId("treat-1");
        doc.setName("Consultation");
        doc.setCategory("General");
        doc.setOrganizationId("org-1");
        return doc;
    }

    @Test
    void indexPatient_callsIndex_onPatientCreated() throws Exception {
        PatientDocument doc = samplePatientDoc();
        searchService.indexPatient(doc, "PatientCreated", null);
        verify(openSearchService).indexPatient(doc);
        verify(sseEmitterService).broadcast("patient");
    }

    @Test
    void indexPatient_callsIndex_onPatientUpdated() throws Exception {
        PatientDocument doc = samplePatientDoc();
        searchService.indexPatient(doc, "PatientUpdated", null);
        verify(openSearchService).indexPatient(doc);
    }

    @Test
    void indexPatient_callsDelete_onPatientDeleted() throws Exception {
        PatientDocument doc = samplePatientDoc();
        searchService.indexPatient(doc, "PatientDeleted", null);
        verify(openSearchService).deletePatient("patient-1", "org-1");
        verify(openSearchService, never()).indexPatient(any());
    }

    @Test
    void indexPatient_skipsBroadcast_whenJobIdPresent() throws Exception {
        PatientDocument doc = samplePatientDoc();
        searchService.indexPatient(doc, "PatientCreated", "job-123");
        verify(openSearchService).indexPatient(doc);
        verify(sseEmitterService, never()).broadcast(any());
    }

    @Test
    void indexPatient_wrapsException_inRuntimeException() throws Exception {
        PatientDocument doc = samplePatientDoc();
        doThrow(new Exception("OpenSearch unavailable")).when(openSearchService).indexPatient(doc);

        assertThatThrownBy(() -> searchService.indexPatient(doc, "PatientCreated", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to index patient");
    }

    @Test
    void indexTreatment_callsIndex_onTreatmentCreated() throws Exception {
        TreatmentDocument doc = sampleTreatmentDoc();
        searchService.indexTreatment(doc, "TreatmentCreated");
        verify(openSearchService).indexTreatment(doc);
        verify(sseEmitterService).broadcast("treatment");
    }

    @Test
    void indexTreatment_callsDelete_onTreatmentDeleted() throws Exception {
        TreatmentDocument doc = sampleTreatmentDoc();
        searchService.indexTreatment(doc, "TreatmentDeleted");
        verify(openSearchService).deleteTreatment("treat-1", "org-1");
        verify(openSearchService, never()).indexTreatment(any());
    }
}
