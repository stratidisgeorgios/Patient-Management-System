package com.patientsystem.searchservice.controller;

import org.springframework.web.bind.annotation.RestController;
import com.patientsystem.searchservice.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import com.patientsystem.searchservice.documents.PatientDocument;
import com.patientsystem.searchservice.documents.TreatmentDocument;
import java.util.List;

@RestController
@Tag(name = "Search Service Controller", description = "Controller for handling search service requests")
@RequestMapping("/api")
public class SearchServiceController {
    private final SearchService searchService;

    public SearchServiceController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search/patients")
    @Operation(summary = "Search patients", description = "Search for patients based on a query string")
    public ResponseEntity<List<PatientDocument>> searchPatients(@RequestParam String q) {
        List<PatientDocument> results = searchService.searchPatients(q);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/treatments")
    @Operation(summary = "Search treatments", description = "Search for treatments based on a query string")
    public ResponseEntity<List<TreatmentDocument>> searchTreatments(@RequestParam String q) {
        List<TreatmentDocument> results = searchService.searchTreatments(q);
        return ResponseEntity.ok(results);
    }
}
