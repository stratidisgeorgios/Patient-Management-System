package com.patientsystem.treatmentservice.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.patientsystem.treatmentservice.dto.TreatmentRequestDTO;
import com.patientsystem.treatmentservice.dto.TreatmentResponseDTO;
import com.patientsystem.treatmentservice.service.TreatmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/api/treatments")
@Tag(name = "Treatment API", description = "API for managing treatments.")
public class TreatmentController {
    private final TreatmentService treatmentService;

    public TreatmentController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @GetMapping
    @Operation(summary = "Get all treatments")
    public ResponseEntity<List<TreatmentResponseDTO>> getAllTreatments() {
        return ResponseEntity.ok(treatmentService.getAllTreatments());
    }

    @GetMapping("/{treatmentId}")
    @Operation(summary = "Get a treatment by ID")
    public ResponseEntity<TreatmentResponseDTO> getTreatmentById(@PathVariable String treatmentId) {
        return ResponseEntity.ok(treatmentService.getTreatmentById(treatmentId));
    }

    @PostMapping
    @Operation(summary = "Create a new treatment")
    public ResponseEntity<TreatmentResponseDTO> createTreatment(@RequestBody TreatmentRequestDTO request) {
        return ResponseEntity.status(201).body(treatmentService.createTreatment(request));
    }

    @PutMapping("/{treatmentId}")
    @Operation(summary = "Update an existing treatment")
    public ResponseEntity<TreatmentResponseDTO> updateTreatment(@PathVariable String treatmentId, @RequestBody TreatmentRequestDTO request) {
        return ResponseEntity.ok(treatmentService.updateTreatment(treatmentId, request));
    }

    @DeleteMapping("/{treatmentId}")
    @Operation(summary = "Delete a treatment")
    public ResponseEntity<Void> deleteTreatment(@PathVariable String treatmentId) {
        treatmentService.deleteTreatment(treatmentId);
        return ResponseEntity.noContent().build();
    }
}
