package com.patientsystem.searchservice.opensearch;

import com.patientsystem.searchservice.documents.PatientDocument;
import com.patientsystem.searchservice.documents.TreatmentDocument;
import jakarta.annotation.PostConstruct;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class OpenSearchService {

    private final OpenSearchClient openSearchClient;

    public OpenSearchService(OpenSearchClient openSearchClient) {
        this.openSearchClient = openSearchClient;
    }

    @PostConstruct
    public void createIndices() throws IOException {
        if (!openSearchClient.indices().exists(b -> b.index("patients")).value()) {
            openSearchClient.indices().create(b -> b
                .index("patients")
                .mappings(m -> m
                    .properties("patientId", p -> p.keyword(k -> k))
                    .properties("name", p -> p.searchAsYouType(s -> s))
                    .properties("email", p -> p.searchAsYouType(s -> s))
                    .properties("dateOfBirth", p -> p.keyword(k -> k))
                    .properties("gender", p -> p.keyword(k -> k))
                )
            );
        }
        if (!openSearchClient.indices().exists(b -> b.index("treatments")).value()) {
            openSearchClient.indices().create(b -> b
                .index("treatments")
                .mappings(m -> m
                    .properties("treatmentId", p -> p.keyword(k -> k))
                    .properties("name", p -> p.searchAsYouType(s -> s))
                    .properties("category", p -> p.keyword(k -> k))
                    .properties("price", p -> p.keyword(k -> k))
                )
            );
        }
    }

    public void indexPatient(PatientDocument doc) throws IOException {
        try {
            openSearchClient.index(IndexRequest.of(i -> i
                .index("patients")
                .id(doc.getPatientId())
                .document(doc)
            ));
        } catch (IOException e) {
            throw new IOException("Failed to index patient: " + e.getMessage(), e);
        }
    }

    public void deletePatient(String patientId) throws IOException {
        try {
            openSearchClient.delete(DeleteRequest.of(d -> d
                .index("patients")
                .id(patientId)
            ));
        } catch (IOException e) {
            throw new IOException("Failed to delete patient: " + e.getMessage(), e);
        }
    }

    public void indexTreatment(TreatmentDocument doc) throws IOException {
        try {
            openSearchClient.index(IndexRequest.of(i -> i
                .index("treatments")
                .id(doc.getTreatmentId())
                .document(doc)
            ));
        } catch (IOException e) {
            throw new IOException("Failed to index treatment: " + e.getMessage(), e);
        }
    }

    public void deleteTreatment(String treatmentId) throws IOException {
        try {
            openSearchClient.delete(DeleteRequest.of(d -> d
                .index("treatments")
                .id(treatmentId)
            ));
        } catch (IOException e) {
            throw new IOException("Failed to delete treatment: " + e.getMessage(), e);
        }
    }

    public List<PatientDocument> searchPatients(String query) throws IOException {
        try {
            SearchResponse<PatientDocument> response = openSearchClient.search(SearchRequest.of(s -> s
                .index("patients")
                .query(q -> q
                    .multiMatch(m -> m
                        .query(query)
                        .fields("name", "name._2gram", "name._3gram", "email", "email._2gram", "email._3gram")
                    )
                )
            ), PatientDocument.class);
            return response.hits().hits().stream().map(Hit::source).toList();
        } catch (IOException e) {
            throw new IOException("Failed to search patients: " + e.getMessage(), e);
        }
    }

    public List<TreatmentDocument> searchTreatments(String query) throws IOException {
        try {
            SearchResponse<TreatmentDocument> response = openSearchClient.search(SearchRequest.of(s -> s
                .index("treatments")
                .query(q -> q
                    .multiMatch(m -> m
                        .query(query)
                        .fields("name", "name._2gram", "name._3gram", "category")
                    )
                )
            ), TreatmentDocument.class);
            return response.hits().hits().stream().map(Hit::source).toList();
        } catch (IOException e) {
            throw new IOException("Failed to search treatments: " + e.getMessage(), e);
        }
    }
}
