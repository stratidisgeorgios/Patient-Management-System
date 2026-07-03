package com.patientsystem.billingservice.controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.patientsystem.billingservice.dto.BillingResponseDTO;
import com.patientsystem.billingservice.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.patientsystem.billingservice.dto.ChargeRequestDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.util.UUID;
@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing Service API", description = "API for managing billing information, including retrieval of patient billing details and charges.")
public class BillingServiceController {
    private final BillingService billingService;

    public BillingServiceController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/{patientId}")
    @Operation(summary = "Get billing information for a patient", description = "Retrieve the billing information for a specific patient identified by their unique ID.")
    public ResponseEntity<BillingResponseDTO> getBillingInfo(@PathVariable String patientId) {
        BillingResponseDTO billingInfo = billingService.getBillingInfo(patientId);
        return ResponseEntity.ok(billingInfo);
    }

    @PostMapping("/{patientId}/charge")
    @Operation(summary = "Add a charge to a patient's billing account", description = "Add a new charge to the billing account of a specific patient identified by their unique ID. The request body")
    public ResponseEntity<Void> addCharge(@PathVariable String patientId, @RequestBody ChargeRequestDTO chargeRequest) {
        billingService.addCharge(patientId, chargeRequest.getTreatmentId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{patientId}/charge/{chargeId}")
    @Operation(summary = "Remove a charge from a patient's billing account", description = "Remove a specific charge from the billing account of a patient identified by their unique ID. The charge to be removed is specified by its unique charge ID.")
    public ResponseEntity<Void> removeCharge(@PathVariable String patientId, @PathVariable UUID chargeId) {
        billingService.removeCharge(patientId, chargeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{patientId}/invoice")
    @Operation(summary = "Generate a PDF invoice for a patient", description = "Generate and download a PDF invoice for a specific patient containing all their charges and total balance.")
    public ResponseEntity<byte[]> getInvoice(@PathVariable String patientId) {
        byte[] pdf = billingService.generateInvoice(patientId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + patientId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
