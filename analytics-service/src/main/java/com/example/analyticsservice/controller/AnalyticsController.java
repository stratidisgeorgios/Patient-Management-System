package com.example.analyticsservice.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.Operation;
import com.example.analyticsservice.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics Service API", description = "API for managing analytics information, including retrieval of patient events and charge events.")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/active-patients")
    @Operation(summary = "Get total active patients", description = "Retrieve the total number of active patients in the system.")
    public ResponseEntity<Long> getTotalActivePatients() {
        long totalActivePatients = analyticsService.getTotalActivePatients();
        return ResponseEntity.ok(totalActivePatients);
    }

    @GetMapping("/patient-registrations/{year}")
    @Operation(summary = "Get patient registrations per month", description = "Retrieve the number of patient registrations per month for a specific year.")
    public ResponseEntity<List<Object[]>> getPatientRegistrationsPerMonth(@PathVariable int year) {
        return ResponseEntity.ok(analyticsService.getPatientRegistrationsPerMonth(year));
    }

    @GetMapping("/average-age")
    @Operation(summary = "Get average age of patients", description = "Retrieve the average age of patients in the system.")
    public ResponseEntity<Double> getAverageAge() { 
        Double averageAge = analyticsService.getAverageAge();
        return ResponseEntity.ok(averageAge);
    }

    @GetMapping("/annual-revenue/{year}")
    @Operation(summary = "Get annual revenue", description = "Retrieve the total revenue generated in a specific year.")
    public ResponseEntity<BigDecimal> getAnnualRevenue(@PathVariable int year) {
        return ResponseEntity.ok(analyticsService.getAnnualRevenue(year));
    }

    @GetMapping("/revenue-per-category")
    @Operation(summary = "Get revenue per category", description = "Retrieve the total revenue generated per treatment category.")
    public ResponseEntity<List<Object[]>> getRevenuePerCategory() {
        return ResponseEntity.ok(analyticsService.getRevenuePerCategory());
    }

    @GetMapping("/most-used-treatments")
    @Operation(summary = "Get most used treatments", description = "Retrieve the most frequently used treatments in the system.")
    public ResponseEntity<List<Object[]>> getMostUsedTreatments() {
        return ResponseEntity.ok(analyticsService.getMostUsedTreatments());
    }

    @GetMapping("/gender-distribution")
    @Operation(summary = "Get gender distribution", description = "Retrieve the percentage distribution of male and female patients.")
    public ResponseEntity<Map<String, Double>> getGenderDistribution() {
        return ResponseEntity.ok(analyticsService.getGenderDistribution());
    }
}
