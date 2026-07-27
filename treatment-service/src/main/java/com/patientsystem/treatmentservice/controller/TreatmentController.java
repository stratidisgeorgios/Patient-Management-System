package com.patientsystem.treatmentservice.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.patientsystem.treatmentservice.dto.TreatmentRequestDTO;
import com.patientsystem.treatmentservice.dto.TreatmentResponseDTO;
import com.patientsystem.treatmentservice.service.TreatmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

@RestController
@RequestMapping("/api/treatments")
@Tag(name = "Treatment API", description = "API for managing treatments.")
public class TreatmentController {
    private final TreatmentService treatmentService;

    public TreatmentController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @GetMapping("/{treatmentId}")
    @Operation(summary = "Get a treatment by ID")
    public ResponseEntity<TreatmentResponseDTO> getTreatmentById(@PathVariable UUID treatmentId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        return ResponseEntity.ok(treatmentService.getTreatmentById(treatmentId, organizationId));
    }

    @PostMapping
    @Operation(summary = "Create a new treatment")
    public ResponseEntity<TreatmentResponseDTO> createTreatment(
            @RequestBody TreatmentRequestDTO request,
            @RequestHeader("X-Organization-Id") String organizationId) {
        return ResponseEntity.status(201).body(treatmentService.createTreatment(request, organizationId));
    }

    @PutMapping("/{treatmentId}")
    @Operation(summary = "Update an existing treatment")
    public ResponseEntity<TreatmentResponseDTO> updateTreatment(
            @PathVariable UUID treatmentId,
            @RequestBody TreatmentRequestDTO request,
            @RequestHeader("X-Organization-Id") String organizationId) {
        return ResponseEntity.ok(treatmentService.updateTreatment(treatmentId, request, organizationId));
    }

    @DeleteMapping("/{treatmentId}")
    @Operation(summary = "Delete a treatment")
    public ResponseEntity<Void> deleteTreatment(
            @PathVariable UUID treatmentId,
            @RequestHeader("X-Organization-Id") String organizationId) {
        treatmentService.deleteTreatment(treatmentId, organizationId);
        return ResponseEntity.noContent().build();
    }
}
