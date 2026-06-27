package com.example.billingservice.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.billingservice.dto.BillingResponseDTO;
import com.example.billingservice.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.billingservice.dto.ChargeRequest;

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
    public ResponseEntity<Void> addCharge(@PathVariable String patientId, @RequestBody ChargeRequest chargeRequest) {
        billingService.addCharge(patientId, chargeRequest.getTreatmentId(), chargeRequest.getPrice());
        return ResponseEntity.ok().build();
    }
}
