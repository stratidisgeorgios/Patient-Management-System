package com.example.billingservice.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.example.billingservice.model.TreatmentCatalog;
import io.swagger.v3.oas.annotations.Operation;

import com.example.billingservice.repository.TreatmentCatalogRepository;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Treatment Catalogue Service API", description = "API for managing treatment catalogue information.")
public class TreatmentCatalogueController {
    private final TreatmentCatalogRepository treatmentCatalogRepository;

    public TreatmentCatalogueController(TreatmentCatalogRepository treatmentCatalogRepository) {
        this.treatmentCatalogRepository = treatmentCatalogRepository;
    }

    @GetMapping("/")
    @Operation(summary = "Get treatment catalogue information", description = "Retrieve the treatment catalogue information for a specific patient identified by their unique ID.")
    public ResponseEntity<List<TreatmentCatalog>> getTreatmentCatalogInfo() {
        List<TreatmentCatalog> treatmentCatalogInfo = treatmentCatalogRepository.findAll();
        return ResponseEntity.ok(treatmentCatalogInfo);
    }

    @PostMapping("/")
    @Operation(summary = "Add a charge to a patient's billing account", description = "Add a new charge to the billing account of a specific patient identified by their unique ID. The request body")
    public ResponseEntity<Void> createTreatment(@RequestBody TreatmentCatalog treatmentCatalog) {
        treatmentCatalogRepository.save(treatmentCatalog);
        return ResponseEntity.ok().build();
    }
    @PutMapping("/{treatmentId}")
    @Operation(summary = "Update treatment information", description = "Update the information of an existing treatment identified by its unique ID. The request body must contain valid updated treatment information.")
    public ResponseEntity<Void> updateTreatment(@PathVariable String treatmentId, @RequestBody TreatmentCatalog treatmentCatalog) {
        treatmentCatalog.setId(treatmentId);
        treatmentCatalogRepository.save(treatmentCatalog);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/{treatmentId}")
    @Operation(summary = "Delete a treatment", description = "Delete an existing treatment from the catalogue identified by its unique ID.")
    public ResponseEntity<Void> deleteTreatment(@PathVariable String treatmentId) {
        treatmentCatalogRepository.deleteById(treatmentId);
        return ResponseEntity.noContent().build();
    }
}
