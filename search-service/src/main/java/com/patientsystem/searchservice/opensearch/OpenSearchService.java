package com.patientsystem.searchservice.opensearch;

import com.patientsystem.searchservice.documents.PatientDocument;
import com.patientsystem.searchservice.documents.TreatmentDocument;
import jakarta.annotation.PostConstruct;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.List;

@Service
public class OpenSearchService {

    private final OpenSearchClient openSearchClient;

    public OpenSearchService(OpenSearchClient openSearchClient) {
        this.openSearchClient = openSearchClient;
    }

    private void ensurePatientIndexExists(String organizationId) throws IOException {
        String index = "patients-" + organizationId;
        if (!openSearchClient.indices().exists(b -> b.index(index)).value()) {
            openSearchClient.indices().create(b -> b
                .index(index)
                .mappings(m -> m
                    .properties("id", p -> p.keyword(k -> k))
                    .properties("name", p -> p.searchAsYouType(s -> s))
                    .properties("email", p -> p.searchAsYouType(s -> s))
                    .properties("dateOfBirth", p -> p.keyword(k -> k))
                    .properties("gender", p -> p.keyword(k -> k))
                )
            );
        }
    }

    private void ensureTreatmentIndexExists(String organizationId) throws IOException {
        String index = "treatments-" + organizationId;
        if (!openSearchClient.indices().exists(b -> b.index(index)).value()) {
            openSearchClient.indices().create(b -> b
                .index(index)
                .mappings(m -> m
                    .properties("id", p -> p.keyword(k -> k))
                    .properties("name", p -> p.searchAsYouType(s -> s))
                    .properties("category", p -> p.searchAsYouType(s -> s))
                    .properties("price", p -> p.keyword(k -> k))
                )
            );
        }
    }

    public void indexPatient(PatientDocument doc) throws IOException {
        try {
            String index = "patients-" + doc.getOrganizationId();
            ensurePatientIndexExists(doc.getOrganizationId());
            openSearchClient.index(IndexRequest.of(i -> i
                .index(index)
                .id(doc.getId())
                .document(doc)
            ));
        } catch (IOException e) {
            throw new IOException("Failed to index patient: " + e.getMessage(), e);
        }
    }

    public void bulkIndexPatients(List<PatientDocument> docs, String organizationId) throws IOException {
        if (docs.isEmpty()) return;
        String index = "patients-" + organizationId;
        ensurePatientIndexExists(organizationId);
        List<BulkOperation> operations = new ArrayList<>();
        for (PatientDocument doc : docs) {
            operations.add(BulkOperation.of(op -> op.index(idx -> idx
                .index(index)
                .id(doc.getId())
                .document(doc)
            )));
        }
        openSearchClient.bulk(BulkRequest.of(b -> b.operations(operations)));
    }

    public void deletePatient(String patientId, String organizationId) throws IOException {
        try {
            String index = "patients-" + organizationId;
            openSearchClient.delete(DeleteRequest.of(d -> d
                .index(index)
                .id(patientId)
            ));
        } catch (IOException e) {
            throw new IOException("Failed to delete patient: " + e.getMessage(), e);
        }
    }

    public void indexTreatment(TreatmentDocument doc) throws IOException {
        try {
            String index = "treatments-" + doc.getOrganizationId();
            ensureTreatmentIndexExists(doc.getOrganizationId());
            openSearchClient.index(IndexRequest.of(i -> i
                .index(index)
                .id(doc.getId())
                .document(doc)
            ));
        } catch (IOException e) {
            throw new IOException("Failed to index treatment: " + e.getMessage(), e);
        }
    }

    public void deleteTreatment(String treatmentId, String organizationId) throws IOException {
        try {
            String index = "treatments-" + organizationId;
            openSearchClient.delete(DeleteRequest.of(d -> d
                .index(index)
                .id(treatmentId)
            ));
        } catch (IOException e) {
            throw new IOException("Failed to delete treatment: " + e.getMessage(), e);
        }
    }

    public List<PatientDocument> searchPatients(String query, String organizationId) throws IOException {
        try {
            String index = "patients-" + organizationId;
            ensurePatientIndexExists(organizationId);
            SearchResponse<PatientDocument> response = openSearchClient.search(SearchRequest.of(s -> s
                .index(index)
                .size(10)
                .query(q -> q
                    .multiMatch(m -> m
                        .query(query)
                        .type(TextQueryType.BoolPrefix)
                        .fields("name", "name._2gram", "name._3gram", "email", "email._2gram", "email._3gram")
                    )
                )
            ), PatientDocument.class);
            return response.hits().hits().stream().map(Hit::source).toList();
        } catch (IOException e) {
            throw new IOException("Failed to search patients: " + e.getMessage(), e);
        }
    }

    public List<TreatmentDocument> searchTreatments(String query, String organizationId) throws IOException {
        try {
            String index = "treatments-" + organizationId;
            ensureTreatmentIndexExists(organizationId);
            SearchResponse<TreatmentDocument> response = openSearchClient.search(SearchRequest.of(s -> s
                .index(index)
                .size(10)
                .query(q -> q
                    .multiMatch(m -> m
                        .query(query)
                        .type(TextQueryType.BoolPrefix)
                        .fields("name", "name._2gram", "name._3gram", "category", "category._2gram", "category._3gram")
                    )
                )
            ), TreatmentDocument.class);
            return response.hits().hits().stream().map(Hit::source).toList();
        } catch (IOException e) {
            throw new IOException("Failed to search treatments: " + e.getMessage(), e);
        }
    }
}
