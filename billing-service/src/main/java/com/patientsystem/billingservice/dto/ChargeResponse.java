package com.patientsystem.billingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChargeResponse {
    private String id;
    private String treatmentId;
    private String treatmentName;
    private String treatmentCategory;
    private BigDecimal price;
    private LocalDateTime timestamp;

    public ChargeResponse(String id, String treatmentId, String treatmentName, String treatmentCategory, BigDecimal price, LocalDateTime timestamp) {
        this.id = id;
        this.treatmentId = treatmentId;
        this.treatmentName = treatmentName;
        this.treatmentCategory = treatmentCategory;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getTreatmentId() { return treatmentId; }
    public String getTreatmentName() { return treatmentName; }
    public String getTreatmentCategory() { return treatmentCategory; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
