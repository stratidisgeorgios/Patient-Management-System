package com.example.billingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChargeResponse {
    private String id;
    private String treatmentName;
    private String category;
    private BigDecimal price;
    private LocalDateTime timestamp;

    public ChargeResponse(String id, String treatmentName, String category, BigDecimal price, LocalDateTime timestamp) {
        this.id = id;
        this.treatmentName = treatmentName;
        this.category = category;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getTreatmentName() { return treatmentName; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
